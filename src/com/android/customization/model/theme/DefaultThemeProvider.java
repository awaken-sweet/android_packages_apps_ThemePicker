/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.customization.model.theme;

import static com.android.customization.model.ResourceConstants.ANDROID_PACKAGE;
import static com.android.customization.model.ResourceConstants.CONFIG_ICON_MASK;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;

import com.android.customization.model.CustomizationManager.OptionsFetchedListener;
import com.android.customization.model.ResourcesApkProvider;
import com.android.customization.model.theme.ThemeBundle.Builder;
import com.android.wallpaper.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of {@link ThemeBundleProvider} that reads Themes' overlays from a stub APK.
 */
public class DefaultThemeProvider extends ResourcesApkProvider implements ThemeBundleProvider {

    private static final String TAG = "DefaultThemeProvider";

    private static final String THEMES_ARRAY = "themes";
    private static final String TITLE_PREFIX = "theme_title_";
    private static final String FONT_PREFIX = "theme_overlay_font_";
    private static final String COLOR_PREFIX = "theme_overlay_color_";
    private static final String SHAPE_PREFIX = "theme_overlay_shape_";
    private static final String ICON_ANDROID_PREFIX = "theme_overlay_icon_android_";
    private static final String ICON_SETTINGS_PREFIX = "theme_overlay_icon_settings_";
    private static final String ICON_SYSTEM_PREFIX = "theme_overlay_icon_system_";
    private static final String ICON_PREVIEW_DRAWABLE_NAME = "ic_wifi_signal_3";

    private static final String DEFAULT_THEME_NAME= "default";

    private static final String ACCENT_COLOR_LIGHT_NAME = "accent_device_default_light";
    private static final String ACCENT_COLOR_DARK_NAME = "accent_device_default_dark";
    private static final String CONFIG_BODY_FONT_FAMILY = "config_bodyFontFamily";
    private static final String CONFIG_HEADLINE_FONT_FAMILY = "config_headlineFontFamily";

    private List<ThemeBundle> mThemes;

    public DefaultThemeProvider(Context context) {
        super(context, context.getString(R.string.themes_stub_package));
    }

    @Override
    public void fetch(OptionsFetchedListener<ThemeBundle> callback, boolean reload) {
        if (mThemes == null || reload) {
            mThemes = new ArrayList<>();
            loadAll();
        }

        if(callback != null) {
            callback.onOptionsLoaded(mThemes);
        }
    }

    private void loadAll() {
        addDefaultTheme();

        String[] themeNames = getItemsFromStub(THEMES_ARRAY);

        for (String themeName : themeNames) {
            // Default theme needs special treatment (see #addDefaultTheme())
            if (DEFAULT_THEME_NAME.equals(themeName)) {
                continue;
            }
            ThemeBundle.Builder builder = new Builder();
            try {
                builder.setTitle(mStubApkResources.getString(
                        mStubApkResources.getIdentifier(TITLE_PREFIX + themeName,
                                "string", mStubPackageName)));

                String fontOverlayPackage = getOverlayPackage(FONT_PREFIX, themeName);

                if (!TextUtils.isEmpty(fontOverlayPackage)) {
                    builder.setFontOverlayPackage(fontOverlayPackage)
                            .setBodyFontFamily(loadTypeface(CONFIG_BODY_FONT_FAMILY,
                                    fontOverlayPackage))
                            .setHeadlineFontFamily(loadTypeface(CONFIG_HEADLINE_FONT_FAMILY,
                                    fontOverlayPackage));
                }

                String colorOverlayPackage = getOverlayPackage(COLOR_PREFIX, themeName);

                if (!TextUtils.isEmpty(colorOverlayPackage)) {
                    builder.setColorPackage(colorOverlayPackage)
                            .setColorAccentLight(loadColor(ACCENT_COLOR_LIGHT_NAME,
                                    colorOverlayPackage))
                            .setColorAccentDark(loadColor(ACCENT_COLOR_DARK_NAME,
                                    colorOverlayPackage));
                }

                String shapeOverlayPackage = getOverlayPackage(SHAPE_PREFIX, themeName);

                if (!TextUtils.isEmpty(shapeOverlayPackage)) {
                    builder.setShapePackage(shapeOverlayPackage)
                            .setShapePath(loadString(CONFIG_ICON_MASK, shapeOverlayPackage));
                } else {
                    builder.setShapePath(mContext.getResources().getString(
                            Resources.getSystem().getIdentifier(CONFIG_ICON_MASK, "string",
                                    ANDROID_PACKAGE)));
                }

                String iconAndroidOverlayPackage = getOverlayPackage(ICON_ANDROID_PREFIX, themeName);

                if (!TextUtils.isEmpty(iconAndroidOverlayPackage)) {
                    builder.addIconPackage(iconAndroidOverlayPackage)
                            .addIcon(loadIconPreviewDrawable(ICON_PREVIEW_DRAWABLE_NAME,
                                    iconAndroidOverlayPackage));
                } else {
                    builder.addIcon(mContext.getResources().getDrawable(
                            Resources.getSystem().getIdentifier(ICON_PREVIEW_DRAWABLE_NAME,
                                    "drawable", ANDROID_PACKAGE), null));
                }

                String iconSystemOverlayPackage = getOverlayPackage(ICON_SYSTEM_PREFIX, themeName);

                if (!TextUtils.isEmpty(iconSystemOverlayPackage)) {
                    builder.addIconPackage(iconSystemOverlayPackage);
                }

                String iconSettingsOverlayPackage = getOverlayPackage(ICON_SETTINGS_PREFIX,
                        themeName);

                if (!TextUtils.isEmpty(iconSettingsOverlayPackage)) {
                    builder.addIconPackage(iconSettingsOverlayPackage);
                }

                mThemes.add(builder.build());
            } catch (NameNotFoundException | NotFoundException e) {
                Log.w(TAG, String.format("Couldn't load part of theme %s, will skip it", themeName),
                        e);
            }
        }
    }

    /**
     * Default theme requires different treatment: if there are overlay packages specified in the
     * stub apk, we'll use those, otherwise we'll get the System default values. But we cannot skip
     * the default theme.
     */
    private void addDefaultTheme() {
        ThemeBundle.Builder builder = new Builder();
        Resources system = Resources.getSystem();

        int titleId = mStubApkResources.getIdentifier(TITLE_PREFIX + DEFAULT_THEME_NAME,
                "string", mStubPackageName);
        if (titleId > 0) {
            builder.setTitle(mStubApkResources.getString(titleId));
        } else {
            builder.setTitle(mContext.getString(R.string.default_theme_title));
        }

        String colorOverlayPackage = getOverlayPackage(COLOR_PREFIX, DEFAULT_THEME_NAME);

        try {
            builder.setColorPackage(colorOverlayPackage)
                    .setColorAccentLight(loadColor(ACCENT_COLOR_LIGHT_NAME, colorOverlayPackage))
                    .setColorAccentDark(loadColor(ACCENT_COLOR_DARK_NAME, colorOverlayPackage));
        } catch (NameNotFoundException | NotFoundException e) {
            Log.i(TAG, "Didn't find color overlay for default theme, will use system default", e);
            int colorAccentLight = system.getColor(
                    system.getIdentifier(ACCENT_COLOR_LIGHT_NAME, "color", ANDROID_PACKAGE), null);
            builder.setColorAccentLight(colorAccentLight);

            int colorAccentDark = system.getColor(
                    system.getIdentifier(ACCENT_COLOR_DARK_NAME, "color", ANDROID_PACKAGE), null);
            builder.setColorAccentDark(colorAccentDark);
            builder.setColorPackage(null);
        }

        String fontOverlayPackage = getOverlayPackage(FONT_PREFIX, DEFAULT_THEME_NAME);

        try {
            builder.setFontOverlayPackage(fontOverlayPackage)
                    .setBodyFontFamily(loadTypeface(CONFIG_BODY_FONT_FAMILY,
                            fontOverlayPackage))
                    .setHeadlineFontFamily(loadTypeface(CONFIG_HEADLINE_FONT_FAMILY,
                            fontOverlayPackage));
        } catch (NameNotFoundException | NotFoundException e) {
            Log.i(TAG, "Didn't find font overlay for default theme, will use system default", e);
            String headlineFontFamily = system.getString(system.getIdentifier(
                    CONFIG_HEADLINE_FONT_FAMILY,"string", ANDROID_PACKAGE));
            String bodyFontFamily = system.getString(system.getIdentifier(CONFIG_BODY_FONT_FAMILY,
                    "string", ANDROID_PACKAGE));
            builder.setHeadlineFontFamily(Typeface.create(headlineFontFamily, Typeface.NORMAL))
                    .setBodyFontFamily(Typeface.create(bodyFontFamily, Typeface.NORMAL));
            builder.setFontOverlayPackage(null);
        }

        try {
            builder.setShapePackage(getOverlayPackage(SHAPE_PREFIX, DEFAULT_THEME_NAME))
                    .setShapePath(loadString(ICON_PREVIEW_DRAWABLE_NAME, colorOverlayPackage));
        } catch (NameNotFoundException | NotFoundException e) {
            Log.i(TAG, "Didn't find shape overlay for default theme, will use system default", e);
            String iconMaskPath = system.getString(system.getIdentifier(CONFIG_ICON_MASK,
                    "string", ANDROID_PACKAGE));
            builder.setShapePath(iconMaskPath);
        }


        try {
            String iconAndroidOverlayPackage = getOverlayPackage(ICON_ANDROID_PREFIX,
                    DEFAULT_THEME_NAME);
            builder.addIconPackage(iconAndroidOverlayPackage)
                    .addIcon(loadIconPreviewDrawable(ICON_ANDROID_PREFIX,
                            iconAndroidOverlayPackage));
        } catch (NameNotFoundException | NotFoundException e) {
            Log.i(TAG, "Didn't find Android icons overlay for default theme, using system default",
                    e);
            builder.addIcon(system.getDrawable(system.getIdentifier(ICON_PREVIEW_DRAWABLE_NAME,
                            "drawable", ANDROID_PACKAGE), null));
        }

        mThemes.add(builder.build());
    }

    private String getOverlayPackage(String prefix, String themeName) {
        return getItemStringFromStub(prefix, themeName);
    }

    private Typeface loadTypeface(String configName, String fontOverlayPackage)
            throws NameNotFoundException, NotFoundException {

        // TODO(santie): check for font being present in system

        Resources overlayRes = mContext.getPackageManager()
                .getResourcesForApplication(fontOverlayPackage);

        String fontFamily = overlayRes.getString(overlayRes.getIdentifier(configName,
                "string", fontOverlayPackage));
        return Typeface.create(fontFamily, Typeface.NORMAL);
    }

    private int loadColor(String colorName, String colorPackage)
            throws NameNotFoundException, NotFoundException {

        Resources overlayRes = mContext.getPackageManager()
                .getResourcesForApplication(colorPackage);
        return overlayRes.getColor(overlayRes.getIdentifier(colorName, "color", colorPackage),
                null);
    }

    private String loadString(String stringName, String packageName)
            throws NameNotFoundException, NotFoundException {

        Resources overlayRes = mContext.getPackageManager().getResourcesForApplication(packageName);
        return overlayRes.getString(overlayRes.getIdentifier(stringName, "string", packageName));
    }

    private Drawable loadIconPreviewDrawable(String drawableName, String packageName)
         throws NameNotFoundException, NotFoundException {

        Resources overlayRes = mContext.getPackageManager().getResourcesForApplication(packageName);
        return overlayRes.getDrawable(
                overlayRes.getIdentifier(drawableName, "drawable", packageName), null);
    }
}
