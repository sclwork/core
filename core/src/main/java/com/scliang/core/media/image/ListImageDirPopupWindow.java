package com.scliang.core.media.image;

import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.scliang.core.R;

import java.util.List;

public class ListImageDirPopupWindow extends BasePopupWindowForListView<ImageFloder>
{
	private ListView mListDir;

	public ListImageDirPopupWindow(int width, int height,
                                   List<ImageFloder> datas, View convertView)
	{
		super(convertView, width, height, true, datas);
	}

	@Override
	public void initViews()
	{
		mListDir = (ListView) findViewById(R.id.id_list_dir);
		mListDir.setAdapter(new CommonAdapter<ImageFloder>(context, mDatas,
				R.layout.view_list_dir_item)
		{
			@Override
			public void convert(PhotoViewHolder helper, ImageFloder item)
			{
				helper.setText(R.id.id_dir_item_name, item.getName().substring(1));
				helper.setImageByUrl(R.id.id_dir_item_image,
						item.getFirstImagePath());
				helper.setText(R.id.id_dir_item_count, item.getCount()+"");
			}
		});

		/**
		 * 点击遮罩层popupwindow消失
		 */
		findViewById(R.id.dirlist_shadow).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
	}

	public interface OnImageDirSelected
	{
		void selected(ImageFloder floder);
	}

	private OnImageDirSelected mImageDirSelected;

	public void setOnImageDirSelected(OnImageDirSelected mImageDirSelected)
	{
		this.mImageDirSelected = mImageDirSelected;
	}

	@Override
	public void initEvents()
	{
		mListDir.setOnItemClickListener(new OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id)
			{

				if (mImageDirSelected != null)
				{
					mImageDirSelected.selected(mDatas.get(position));
				}
			}
		});
	}

	@Override
	public void init()
	{

	}

	@Override
	protected void beforeInitWeNeedSomeParams(Object... params)
	{
	}


	public void setListViewHeight(){
		CommonAdapter listAdapter = (CommonAdapter) mListDir.getAdapter();
		if (listAdapter == null) {
			return;
		}

		int totalHeight = 0;
		for (int i = 0; i < listAdapter.getCount(); i++) {
			View listItem = listAdapter.getView(i, null, mListDir);
			listItem.measure(0, 0);
			totalHeight += listItem.getMeasuredHeight();
		}

		ViewGroup.LayoutParams params = mListDir.getLayoutParams();
		params.height = totalHeight + (mListDir.getDividerHeight() * (listAdapter.getCount()-1));
		((ViewGroup.MarginLayoutParams)params).setMargins(10, 10, 10, 10);
		mListDir.setLayoutParams(params);
	}

}
