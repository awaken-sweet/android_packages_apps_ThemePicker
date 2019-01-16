/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.customization.picker.grid;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.android.customization.model.grid.GridOption;
import com.android.customization.model.grid.GridOptionsManager;
import com.android.customization.picker.BasePreviewAdapter;
import com.android.customization.picker.BasePreviewAdapter.PreviewPage;
import com.android.customization.widget.OptionSelectorController;
import com.android.customization.widget.PreviewPager;
import com.android.wallpaper.R;
import com.android.wallpaper.asset.Asset;
import com.android.wallpaper.asset.ContentUriAsset;
import com.android.wallpaper.picker.ToolbarFragment;

import com.bumptech.glide.request.RequestOptions;

/**
 * Fragment that contains the UI for selecting and applying a GridOption.
 */
public class GridFragment extends ToolbarFragment {

    public static GridFragment newInstance(CharSequence title, GridOptionsManager manager) {
        GridFragment fragment = new GridFragment();
        fragment.setManager(manager);
        fragment.setArguments(ToolbarFragment.createArguments(title));
        return fragment;
    }

    private RecyclerView mOptionsContainer;
    private OptionSelectorController mOptionsController;
    private GridOptionsManager mGridManager;
    private GridOption mSelectedOption;
    private PreviewPager mPreviewPager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(
                R.layout.fragment_grid_picker, container, /* attachToRoot */ false);
        setUpToolbar(view);
        mPreviewPager = view.findViewById(R.id.grid_preview_pager);
        mOptionsContainer = view.findViewById(R.id.options_container);
        setUpOptions();

        return view;
    }

    private void setManager(GridOptionsManager manager) {
        mGridManager = manager;
    }

    private void createAdapter() {
        mPreviewPager.setAdapter(new GridPreviewAdapter(mSelectedOption));
    }

    private void setUpOptions() {
        mGridManager.fetchOptions(options -> {
            mOptionsController = new OptionSelectorController(mOptionsContainer, options);

            mOptionsController.addListener(selected -> {
                mSelectedOption = (GridOption) selected;
                createAdapter();
            });
            mOptionsController.initOptions();
            for (GridOption option : options) {
                if (option.isActive(getContext())) {
                    mSelectedOption = option;
                }
            }
            // For development only, as there should always be a grid set.
            if (mSelectedOption == null) {
                mSelectedOption = options.get(0);
            }
            createAdapter();
        });
    }

    private static class GridPreviewPage extends PreviewPage {
        private final int mPageId;
        private final Asset mPreviewAsset;
        private final int mCols;
        private final int mRows;
        private final Activity mActivity;

        private GridPreviewPage(Activity activity, int id, Uri previewUri, int rows, int cols) {
            super(null);
            mPageId = id;
            mPreviewAsset = new ContentUriAsset(activity, previewUri,
                    RequestOptions.fitCenterTransform());
            mRows = rows;
            mCols = cols;
            mActivity = activity;
        }

        public void bindPreviewContent() {
            mPreviewAsset.loadDrawable(mActivity, card.findViewById(R.id.grid_preview_image),
                    card.getContext().getResources().getColor(R.color.primary_color, null));
        }
    }
    /**
     * Adapter class for mPreviewPager.
     * This is a ViewPager as it allows for a nice pagination effect (ie, pages snap on swipe,
     * we don't want to just scroll)
     */
    class GridPreviewAdapter extends BasePreviewAdapter<GridPreviewPage> {

        GridPreviewAdapter(GridOption gridOption) {
            super(getContext(), R.layout.grid_preview_card);
            for (int i = 0; i < gridOption.previewPagesCount; i++) {
                addPage(new GridPreviewPage(getActivity(), i,
                        gridOption.previewImageUri.buildUpon().appendPath("" + i).build(),
                        gridOption.rows, gridOption.cols));
            }
        }
    }
}
