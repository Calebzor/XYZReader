package com.example.xyzreader.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Html;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment
		implements LoaderManager.LoaderCallbacks<Cursor> {

	private static final String TAG = "ArticleDetailFragment";

	public static final String ARG_ITEM_ID = "item_id";

	private Cursor mCursor;
	private long mItemId;
	private View mRootView;

	@BindView(R.id.article_body)
	TextView articleBody;

	@BindView(R.id.article_byline)
	TextView articleByline;

	private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
	// Use default locale format
	private SimpleDateFormat outputFormat = new SimpleDateFormat();
	// Most time functions can only handle 1902 - 2037
	private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2, 1, 1);
	private Unbinder unbinder;

	public static ArticleDetailFragment newInstance(long itemId) {
		Bundle arguments = new Bundle();
		arguments.putLong(ARG_ITEM_ID, itemId);
		ArticleDetailFragment fragment = new ArticleDetailFragment();
		fragment.setArguments(arguments);
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getArguments().containsKey(ARG_ITEM_ID)) {
			mItemId = getArguments().getLong(ARG_ITEM_ID);
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		// In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
		// the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
		// fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
		// we do this in onActivityCreated.
		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);
		unbinder = ButterKnife.bind(this, mRootView);

		return mRootView;
	}

	private Date parsePublishedDate() {
		try {
			String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
			return dateFormat.parse(date);
		}
		catch (ParseException ex) {
			Log.e(TAG, ex.getMessage());
			Log.i(TAG, "passing today's date");
			return new Date();
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		unbinder.unbind();
	}

	private void bindViews() {
		if (mRootView == null) {
			return;
		}

		if (mCursor == null) {
			mRootView.setVisibility(View.GONE);
			String notAvailable = getString(R.string.not_available);
			articleByline.setText(notAvailable);
			articleBody.setText(notAvailable);
			return;
		}

		articleByline.setMovementMethod(new LinkMovementMethod());
		Date publishedDate = parsePublishedDate();
		String byline;
		if (!publishedDate.before(START_OF_EPOCH.getTime())) {
			byline = DateUtils.getRelativeTimeSpanString(publishedDate.getTime(),
					System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
					DateUtils.FORMAT_ABBREV_ALL).toString();
		}
		else {
			// If date is before 1902, just show the string
			byline = outputFormat.format(publishedDate);

		}
		articleByline.setText(byline + " - " + mCursor.getString(ArticleLoader.Query.AUTHOR));
		String body = mCursor.getString(ArticleLoader.Query.BODY);
		articleBody.setText(Html.fromHtml(body.replaceAll("(\r\n\r\n|\n\n)", "<br /><br />")));
	}

	@Override
	public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
		return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
		if (!isAdded()) {
			if (cursor != null) {
				cursor.close();
			}
			return;
		}

		mCursor = cursor;
		if (mCursor != null && !mCursor.moveToFirst()) {
			Log.e(TAG, "Error reading item detail cursor");
			mCursor.close();
			mCursor = null;
		}

		bindViews();
	}

	@Override
	public void onLoaderReset(Loader<Cursor> cursorLoader) {
		mCursor = null;
	}

}
