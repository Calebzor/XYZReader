package com.example.xyzreader.ui;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.app.ShareCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.squareup.picasso.Picasso;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * An activity representing a single Article detail screen, letting you swipe between articles.
 */
public class ArticleDetailActivity extends AppCompatActivity
		implements LoaderManager.LoaderCallbacks<Cursor> {

	private Cursor mCursor;
	private long mStartId;

	@BindView(R.id.toolbar)
	Toolbar toolbar;

	@BindView(R.id.collapsingToolbar)
	CollapsingToolbarLayout collapsingToolbarLayout;

	@BindView(R.id.photo)
	ImageView ivPhoto;

	@BindView(R.id.pager)
	ViewPager mPager;

	@BindView(R.id.tabs)
	TabLayout tabLayout;

	@BindView(R.id.shareFab)
	FloatingActionButton shareFab;

	private MyPagerAdapter mPagerAdapter;

	private String currentTabShareText = "";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_article_detail);
		ButterKnife.bind(this);

		getLoaderManager().initLoader(0, null, this);

		setSupportActionBar(toolbar);
		ActionBar supportActionBar = getSupportActionBar();
		supportActionBar.setDisplayHomeAsUpEnabled(true);

		mPagerAdapter = new MyPagerAdapter(getFragmentManager());
		mPager.setAdapter(mPagerAdapter);

		setPagerOnPageChangeListener();
		setFabAction();

		if (savedInstanceState == null && (getIntent() != null && getIntent().getData() != null)) {
			mStartId = ItemsContract.Items.getItemId(getIntent().getData());
		}
	}

	private void setFabAction() {
		shareFab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				shareAction();
			}
		});
	}

	private void setPagerOnPageChangeListener() {
		mPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {

			@Override
			public void onPageSelected(int position) {
				if (mCursor != null) {
					mCursor.moveToPosition(position);
					String title = mCursor.getString(ArticleLoader.Query.TITLE);
					String author = mCursor.getString(ArticleLoader.Query.AUTHOR);
					collapsingToolbarLayout.setTitle(title);
					Picasso.with(ArticleDetailActivity.this).load(
							mCursor.getString(ArticleLoader.Query.PHOTO_URL)).placeholder(
							R.drawable.empty_detail).placeholder(R.drawable.banner_1920).into(
							ivPhoto);
					currentTabShareText = author + ": " + title;
				}
			}
		});
	}

	private void shareAction() {
		startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(
				ArticleDetailActivity.this).setType("text/plain")
						.setText(getString(R.string.share_Text) + ": " + currentTabShareText).getIntent(),
				getString(R.string.action_share)));
	}

	@Override
	public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
		return ArticleLoader.newAllArticlesInstance(this);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
		mCursor = cursor;
		mPagerAdapter.notifyDataSetChanged();

		// Select the start ID
		if (mStartId > 0) {
			mCursor.moveToFirst();
			// TODO: optimize
			while (!mCursor.isAfterLast()) {
				if (mCursor.getLong(ArticleLoader.Query._ID) == mStartId) {
					final int position = mCursor.getPosition();
					mPager.setCurrentItem(position, false);
					tabLayout.setSmoothScrollingEnabled(true);
					scrollToSelectedPosition();
					break;
				}
				mCursor.moveToNext();
			}
			mStartId = 0;
		}
	}

	private void scrollToSelectedPosition() {
		final int selectedTabPosition = tabLayout.getSelectedTabPosition();
		new Handler().post(new Runnable() {
			@Override
			public void run() {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (tabLayout != null) {
							tabLayout.setScrollPosition(selectedTabPosition, 0f, true);
						}
					}
				});
			}
		});
	}

	@Override
	public void onLoaderReset(Loader<Cursor> cursorLoader) {
		mCursor = null;
		mPagerAdapter.notifyDataSetChanged();
	}

	private class MyPagerAdapter extends FragmentStatePagerAdapter {

		public MyPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			mCursor.moveToPosition(position);
			return ArticleDetailFragment.newInstance(mCursor.getLong(ArticleLoader.Query._ID));
		}

		@Override
		public CharSequence getPageTitle(int position) {
			mCursor.moveToPosition(position);

			return mCursor.getString(ArticleLoader.Query.TITLE);
		}

		@Override
		public int getCount() {
			return (mCursor != null) ? mCursor.getCount() : 0;
		}
	}
}
