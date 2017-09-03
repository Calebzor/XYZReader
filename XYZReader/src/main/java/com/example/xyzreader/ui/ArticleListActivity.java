package com.example.xyzreader.ui;

import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.data.UpdaterService;
import com.squareup.picasso.Picasso;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends AppCompatActivity
		implements LoaderManager.LoaderCallbacks<Cursor> {

	private static final String TAG = ArticleListActivity.class.toString();

	@BindView(R.id.swipe_refresh_layout)
	SwipeRefreshLayout mSwipeRefreshLayout;

	@BindView(R.id.recycler_view)
	RecyclerView mRecyclerView;

	@BindView(R.id.collapsing_toolbar)
	CollapsingToolbarLayout collapsingToolbarLayout;

	@BindView(R.id.coordinatorLayout)
	CoordinatorLayout coordinatorLayout;

	private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
	// Use default locale format
	private SimpleDateFormat outputFormat = new SimpleDateFormat();
	// Most time functions can only handle 1902 - 2037
	private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2, 1, 1);
	private Unbinder bind;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_article_list);

		bind = ButterKnife.bind(this);

		getLoaderManager().initLoader(0, null, this);

		if (savedInstanceState == null) {
			refresh();
		}

		collapsingToolbarLayout.setTitle(getString(R.string.app_name));
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		bind.unbind();
	}

	private void refresh() {
		startService(new Intent(this, UpdaterService.class));
	}

	@Override
	protected void onStart() {
		super.onStart();
		IntentFilter filter = new IntentFilter();
		filter.addAction(UpdaterService.BROADCAST_ACTION_STATE_CHANGE);
		filter.addAction(UpdaterService.BROADCAST_ACTION_NO_INTERNET);
		registerReceiver(mRefreshingReceiver, filter);
	}

	@Override
	protected void onStop() {
		super.onStop();
		unregisterReceiver(mRefreshingReceiver);
	}

	private boolean mIsRefreshing = false;

	private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
				mIsRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
				updateRefreshingUI();
			}
			else if (UpdaterService.BROADCAST_ACTION_NO_INTERNET.equals(intent.getAction()) &&
					!isConnected()) {
				Snackbar snackbar = Snackbar.make(coordinatorLayout, R.string.network_error,
						Snackbar.LENGTH_LONG);
				snackbar.show();
			}
		}
	};

	private boolean isConnected() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo ni = cm.getActiveNetworkInfo();
		return !(ni == null || !ni.isConnected());
	}

	private void updateRefreshingUI() {
		mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
		return ArticleLoader.newAllArticlesInstance(this);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
		Adapter adapter = new Adapter(cursor);
		adapter.setHasStableIds(true);
		mRecyclerView.setAdapter(adapter);
		int columnCount = getResources().getInteger(R.integer.list_column_count);
		StaggeredGridLayoutManager sglm = new StaggeredGridLayoutManager(columnCount,
				StaggeredGridLayoutManager.VERTICAL);
		mRecyclerView.setLayoutManager(sglm);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		if (mRecyclerView != null) {
			mRecyclerView.setAdapter(null);
		}
	}

	private class Adapter extends RecyclerView.Adapter<ViewHolder> {

		private Cursor mCursor;

		public Adapter(Cursor cursor) {
			mCursor = cursor;
		}

		@Override
		public long getItemId(int position) {
			mCursor.moveToPosition(position);
			return mCursor.getLong(ArticleLoader.Query._ID);
		}

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View view = getLayoutInflater().inflate(R.layout.list_item_article, parent, false);
			final ViewHolder vh = new ViewHolder(view);
			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					startActivity(new Intent(Intent.ACTION_VIEW,
							ItemsContract.Items.buildItemUri(getItemId(vh.getAdapterPosition()))));
				}
			});
			return vh;
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
		public void onBindViewHolder(ViewHolder holder, int position) {
			mCursor.moveToPosition(position);
			holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
			Date publishedDate = parsePublishedDate();
			String subtitle;
			if (!publishedDate.before(START_OF_EPOCH.getTime())) {
				subtitle = DateUtils.getRelativeTimeSpanString(publishedDate.getTime(),
						System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
						DateUtils.FORMAT_ABBREV_ALL).toString();
			}
			else {
				subtitle = outputFormat.format(publishedDate);
			}
			holder.subtitleView.setText(
					subtitle + "\n" + mCursor.getString(ArticleLoader.Query.AUTHOR));
			Picasso.with(holder.thumbnailView.getContext()).load(
					mCursor.getString(ArticleLoader.Query.THUMB_URL)).placeholder(
					R.drawable.empty_detail).into(holder.thumbnailView);
		}

		@Override
		public int getItemCount() {
			return mCursor.getCount();
		}
	}

	public static class ViewHolder extends RecyclerView.ViewHolder {

		public ImageView thumbnailView;
		public TextView titleView;
		public TextView subtitleView;

		public ViewHolder(View view) {
			super(view);
			thumbnailView = (ImageView) view.findViewById(R.id.thumbnail);
			titleView = (TextView) view.findViewById(R.id.article_title);
			subtitleView = (TextView) view.findViewById(R.id.article_subtitle);
		}
	}
}
