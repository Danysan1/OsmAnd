package net.osmand.plus.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.widget.Toast;

import androidx.annotation.NonNull;

import net.osmand.core.android.MapRendererView;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager;
import net.osmand.map.TileSourceManager.TileSourceTemplate;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.mapillary.MapillaryPlugin;
import net.osmand.plus.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.util.MapUtils;

import net.osmand.util.Algorithms;
import net.osmand.core.android.TileSourceProxyProvider;
import net.osmand.core.jni.MapLayerConfiguration;
import net.osmand.data.LatLon;
import net.osmand.core.jni.PointI;

public class MapTileLayer extends BaseMapLayer {

	public static final int OVERZOOM_IN = 2;
	private static final int ADDITIONAL_TILE_CACHE = 3;

	protected final boolean mainMap;
	protected ITileSource map = null;
	protected MapTileAdapter mapTileAdapter = null;

	protected Paint paintBitmap;
	protected RectF bitmapToDraw = new RectF();
	protected Rect bitmapToZoom = new Rect();

	protected OsmandMapTileView view;
	protected ResourceManager resourceManager;
	protected OsmandSettings settings;
	private boolean visible = true;
	private boolean useSampling;

	private boolean oldRender = false;
	private String cachedUnderlay;
	private Integer cachedMapTransparency;
	private String cachedOverlay;
	private Integer cachedOverlayTransparency;

	public MapTileLayer(@NonNull Context context, boolean mainMap) {
		super(context);
		this.mainMap = mainMap;
	}
	
	@Override
	public boolean drawInScreenPixels() {
		return false;
	}

	@Override
	public void initLayer(@NonNull OsmandMapTileView view) {
		this.view = view;
		settings = view.getSettings();
		resourceManager = view.getApplication().getResourceManager();

		useSampling = Build.VERSION.SDK_INT < 28;

		paintBitmap = new Paint();
		paintBitmap.setFilterBitmap(true);
		paintBitmap.setAlpha(getAlpha());
		
		if (mapTileAdapter != null) {
			mapTileAdapter.initLayerAdapter(this, view);
		}
	}
	
	@Override
	public void setAlpha(int alpha) {
		super.setAlpha(alpha);
		if (paintBitmap != null) {
			paintBitmap.setAlpha(alpha);
		}/*
		final MapRendererView mapRenderer = view.getMapRenderer();
		if (mapRenderer != null && !oldRender) {
			OsmandSettings st = view.getApplication().getSettings();
			if (!Algorithms.objectEquals(st.MAP_TRANSPARENCY.get(), cachedMapTransparency)) {
				cachedMapTransparency = st.MAP_TRANSPARENCY.get();
				MapLayerConfiguration mapLayerConfiguration = new MapLayerConfiguration();
				mapLayerConfiguration.setOpacityFactor(((float) cachedMapTransparency) / 255.0f);
				mapRenderer.setMapLayerConfiguration(0, mapLayerConfiguration);
			}
			if (!Algorithms.objectEquals(st.MAP_OVERLAY_TRANSPARENCY.get(), cachedOverlayTransparency)) {
				cachedOverlayTransparency = st.MAP_OVERLAY_TRANSPARENCY.get();
				MapLayerConfiguration mapLayerConfiguration = new MapLayerConfiguration();
				mapLayerConfiguration.setOpacityFactor(((float) cachedOverlayTransparency) / 255.0f);
				mapRenderer.setMapLayerConfiguration(1, mapLayerConfiguration);
			}
		}//*/
	}
	
	public void setMapTileAdapter(MapTileAdapter mapTileAdapter) {
		if (this.mapTileAdapter == mapTileAdapter) {
			return;
		}
		if (this.mapTileAdapter != null) {
			this.mapTileAdapter.onClear();
		}
		this.mapTileAdapter = mapTileAdapter;
		if (mapTileAdapter != null && view != null) {
			mapTileAdapter.initLayerAdapter(this, view);
			mapTileAdapter.onInit();
		}
	}
	
	public void setMapForMapTileAdapter(ITileSource map, MapTileAdapter mapTileAdapter) {
		if (mapTileAdapter == this.mapTileAdapter) {
			this.map = map;
		}
	}
	
	public void setMap(ITileSource map) {
		MapTileAdapter target = null;
		if (map instanceof TileSourceTemplate) {
			if (TileSourceManager.RULE_YANDEX_TRAFFIC.equals(((TileSourceTemplate) map).getRule())) {
				map = null;
				target = new YandexTrafficAdapter();
			}
			
		}
		this.map = map;
		setMapTileAdapter(target);
		/*
		final MapRendererView mapRenderer = view.getMapRenderer();
		if (mapRenderer != null && !oldRender) {
			OsmandSettings st = view.getApplication().getSettings();
			if (!Algorithms.objectEquals(st.MAP_UNDERLAY.get(), cachedUnderlay)) {
				cachedUnderlay = st.MAP_UNDERLAY.get();
				ITileSource tileSource = st.getTileSourceByName(cachedUnderlay, false);
				if (tileSource != null) {
					TileSourceProxyProvider prov = new TileSourceProxyProvider(view.getApplication(), tileSource);
					mapRenderer.setMapLayerProvider(-1, prov.instantiateProxy(true));
					prov.swigReleaseOwnership();
					// mapRenderer.setMapLayerProvider(-1,
					// net.osmand.core.jni.OnlineTileSources.getBuiltIn().createProviderFor("Mapnik (OsmAnd)"));
				} else {
					mapRenderer.resetMapLayerProvider(-1);
				}
			}
			if (!Algorithms.objectEquals(st.MAP_OVERLAY.get(), cachedOverlay)) {
				cachedOverlay = st.MAP_OVERLAY.get();
				ITileSource tileSource = st.getTileSourceByName(cachedOverlay, false);
				if (tileSource != null) {
					TileSourceProxyProvider prov = new TileSourceProxyProvider(view.getApplication(), tileSource);
					mapRenderer.setMapLayerProvider(1, prov.instantiateProxy(true));
					prov.swigReleaseOwnership();
					// mapRenderer.setMapLayerProvider(1,
					// net.osmand.core.jni.OnlineTileSources.getBuiltIn().createProviderFor("Mapnik (OsmAnd)"));
				} else {
					mapRenderer.resetMapLayerProvider(1);
				}
			}
		}//*/
	}
	
	public MapTileAdapter getMapTileAdapter() {
		return mapTileAdapter;
	}
	
	@SuppressLint("WrongCall")
	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings drawSettings) {
		if ((map == null && mapTileAdapter == null) || !visible) {
			return;
		}
		if (mapTileAdapter != null) {
			mapTileAdapter.onDraw(canvas, tileBox, drawSettings);
		}
		drawTileMap(canvas, tileBox, drawSettings);

		final MapRendererView mapRenderer = view.getMapRenderer();
		if (mapRenderer != null && !oldRender) {
			OsmandSettings st = view.getApplication().getSettings();//*
			if (!Algorithms.objectEquals(st.MAP_UNDERLAY.get(), cachedUnderlay)) {
				cachedUnderlay = st.MAP_UNDERLAY.get();
				//ITileSource tileSource = st.getTileSourceByName(cachedUnderlay, false);
				if (tileSource != null) {
					TileSourceProxyProvider prov = new TileSourceProxyProvider(view.getApplication(), map);
					mapRenderer.setMapLayerProvider(-1, prov.instantiateProxy(true));
					prov.swigReleaseOwnership();
				} else {
					mapRenderer.resetMapLayerProvider(-1);
				}
			}//*/
			if (!Algorithms.objectEquals(st.MAP_TRANSPARENCY.get(), cachedMapTransparency)) {
				cachedMapTransparency = st.MAP_TRANSPARENCY.get();
				MapLayerConfiguration mapLayerConfiguration = new MapLayerConfiguration();
				mapLayerConfiguration.setOpacityFactor(((float) cachedMapTransparency) / 255.0f);
				mapRenderer.setMapLayerConfiguration(0, mapLayerConfiguration);
			}//*/
			if (!Algorithms.objectEquals(st.MAP_OVERLAY.get(), cachedOverlay)) {
				cachedOverlay = st.MAP_OVERLAY.get();
				//ITileSource tileSource = st.getTileSourceByName(cachedOverlay, false);
				if (tileSource != null) {
					TileSourceProxyProvider prov = new TileSourceProxyProvider(view.getApplication(), map);
					mapRenderer.setMapLayerProvider(1, prov.instantiateProxy(true));
					prov.swigReleaseOwnership();
				} else {
					mapRenderer.resetMapLayerProvider(1);
				}
			}//*/
			if (!Algorithms.objectEquals(st.MAP_OVERLAY_TRANSPARENCY.get(), cachedOverlayTransparency)) {
				cachedOverlayTransparency = st.MAP_OVERLAY_TRANSPARENCY.get();
				MapLayerConfiguration mapLayerConfiguration = new MapLayerConfiguration();
				mapLayerConfiguration.setOpacityFactor(((float) cachedOverlayTransparency) / 255.0f);
				mapRenderer.setMapLayerConfiguration(1, mapLayerConfiguration);
			}//*/
			// opengl renderer
			LatLon ll = tileBox.getLatLonFromPixel(tileBox.getPixWidth() / 2, tileBox.getPixHeight() / 2);
			mapRenderer.setTarget(new PointI(MapUtils.get31TileNumberX(ll.getLongitude()), MapUtils.get31TileNumberY(ll
					.getLatitude())));
			mapRenderer.setAzimuth(-tileBox.getRotate());
			mapRenderer.setZoom((float) (tileBox.getZoom() + tileBox.getZoomAnimation() + tileBox
					.getZoomFloatPart()));
			float zoomMagnifier = st.MAP_DENSITY.get();
			mapRenderer.setVisualZoomShift(zoomMagnifier - 1.0f);
		}

	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings drawSettings) {
		
	}

	public void drawTileMap(Canvas canvas, RotatedTileBox tileBox, DrawSettings drawSettings) {
		ITileSource map = this.map; 
		if (map == null) {
			return;
		}
		ResourceManager mgr = resourceManager;
		int nzoom = tileBox.getZoom();
		final QuadRect tilesRect = tileBox.getTileBounds();

		// recalculate for ellipsoid coordinates
		float ellipticTileCorrection  = 0;
		if (map.isEllipticYTile()) {
			ellipticTileCorrection = (float) (MapUtils.getTileEllipsoidNumberY(nzoom, tileBox.getLatitude()) - tileBox.getCenterTileY());
		}

		int left = (int) Math.floor(tilesRect.left);
		int top = (int) Math.floor(tilesRect.top + ellipticTileCorrection);
		int width = (int) Math.ceil(tilesRect.right - left);
		int height = (int) Math.ceil(tilesRect.bottom + ellipticTileCorrection - top);

		int tiles = (width + ADDITIONAL_TILE_CACHE) * (height + ADDITIONAL_TILE_CACHE);
		mgr.setMapTileLayerSizes(this, tiles);

		boolean useInternet = (OsmandPlugin.isActive(OsmandRasterMapsPlugin.class) || OsmandPlugin.isActive(MapillaryPlugin.class))
				&& settings.isInternetConnectionAvailable() && map.couldBeDownloadedFromInternet();
		int maxLevel = map.getMaximumZoomSupported();
		int tileSize = map.getTileSize();
		boolean oneTileShown = false;

		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				int leftPlusI = left + i;
				int topPlusJ = top + j;

				int x1 = tileBox.getPixXFromTileXNoRot(leftPlusI);
				int x2 = tileBox.getPixXFromTileXNoRot(leftPlusI + 1);

				int y1 = tileBox.getPixYFromTileYNoRot(topPlusJ - ellipticTileCorrection);
				int y2 = tileBox.getPixYFromTileYNoRot(topPlusJ + 1 - ellipticTileCorrection);
				bitmapToDraw.set(x1, y1, x2 , y2);
				
				final int tileX = leftPlusI;
				final int tileY = topPlusJ;
				Bitmap bmp = null;
				String ordImgTile = mgr.calculateTileId(map, tileX, tileY, nzoom);
				// asking tile image async
				boolean imgExist = mgr.tileExistOnFileSystem(ordImgTile, map, tileX, tileY, nzoom);
				boolean originalWillBeLoaded = useInternet && nzoom <= maxLevel;
				if (imgExist || originalWillBeLoaded) {
					bmp = mgr.getBitmapTilesCache().getTileForMapAsync(ordImgTile, map, tileX, tileY,
							nzoom, useInternet, drawSettings.mapRefreshTimestamp);
				}
				if (bmp == null) {
					int div = 1;
					boolean readFromCache = originalWillBeLoaded || imgExist;
					boolean loadIfExists = !readFromCache;
					// asking if there is small version of the map (in cache)
					int allowedScale = Math.min(OVERZOOM_IN + Math.max(0, nzoom - map.getMaximumZoomSupported()), 8);
					int kzoom = 1;
					for (; kzoom <= allowedScale; kzoom++) {
						div *= 2;
						int x = tileX / div;
						int y = tileY / div;
						int zoom = nzoom - kzoom;
						String imgTileId = mgr.calculateTileId(map, x, y, zoom);
						if (readFromCache) {
							bmp = mgr.getBitmapTilesCache().get(imgTileId, drawSettings.mapRefreshTimestamp);
							if (bmp != null) {
								break;
							}
						} else if (loadIfExists) {
							if (mgr.tileExistOnFileSystem(imgTileId, map, x, y, zoom)
									|| (useInternet && zoom <= maxLevel)) {
								bmp = mgr.getBitmapTilesCache().getTileForMapAsync(imgTileId, map, x, y,
										zoom, useInternet, drawSettings.mapRefreshTimestamp);
								break;
							}
						}

					}
					if (bmp != null) {
						if (bmp.getWidth() != tileSize && bmp.getWidth() > 0) {
							tileSize = bmp.getWidth();
						}
						int xZoom = (tileX % div) * tileSize / div;
						int yZoom = (tileY % div) * tileSize / div;
						// nice scale
						boolean useSampling = this.useSampling && kzoom > 3;
						bitmapToZoom.set(Math.max(xZoom, 0), Math.max(yZoom, 0), 
								Math.min(xZoom + tileSize / div, tileSize), 
								Math.min(yZoom + tileSize / div, tileSize));
						if (!useSampling) {
							canvas.drawBitmap(bmp, bitmapToZoom, bitmapToDraw, paintBitmap);
						} else {
							int margin = 1;
							int scaledSize = tileSize / div;
							float innerMargin = 0.5f;
							RectF src = new RectF(0, 0, scaledSize, scaledSize);
							if (bitmapToZoom.left >= margin) {
								bitmapToZoom.left -= margin;
								src.left = innerMargin;
								src.right += margin;
							}
							if (bitmapToZoom.top >= margin) {
								bitmapToZoom.top -= margin;
								src.top = innerMargin;
								src.bottom += margin;
							}
							if (bitmapToZoom.right + margin <= tileSize) {
								bitmapToZoom.right += margin;
								src.right += margin - innerMargin;
							}
							if (bitmapToZoom.bottom + margin <= tileSize) {
								bitmapToZoom.bottom += margin;
								src.bottom += margin - innerMargin;
							}
							Matrix m = new Matrix();
							RectF dest = new RectF(0, 0, tileSize, tileSize);
							m.setRectToRect(src, dest, Matrix.ScaleToFit.FILL);
							Bitmap sampled = Bitmap.createBitmap(bmp, 
									bitmapToZoom.left, bitmapToZoom.top, 
									bitmapToZoom.width(), bitmapToZoom.height(), m, true);
							bitmapToZoom.set(0, 0, tileSize, tileSize);
							// very expensive that's why put in the cache
							mgr.getBitmapTilesCache().put(ordImgTile, sampled, drawSettings.mapRefreshTimestamp);
							canvas.drawBitmap(sampled, bitmapToZoom, bitmapToDraw, paintBitmap);
						}
					}
				} else {
					bitmapToZoom.set(0, 0, tileSize, tileSize);
					canvas.drawBitmap(bmp, bitmapToZoom, bitmapToDraw, paintBitmap);
				}
				if (bmp != null) {
					oneTileShown = true;
				}
			}
		}
		
		if (mainMap && !oneTileShown && !useInternet && warningToSwitchMapShown < 3) {
			if (resourceManager.getRenderer().containsLatLonMapData(view.getLatitude(), view.getLongitude(), nzoom)) {
				Toast.makeText(view.getContext(), R.string.switch_to_vector_map_to_see, Toast.LENGTH_LONG).show();
				warningToSwitchMapShown++;
			}
		}
	}
	

	@Override
	public int getMaximumShownMapZoom() {
		return map == null ? 20 : map.getMaximumZoomSupported() + OVERZOOM_IN;
	}
	
	@Override
	public int getMinimumShownMapZoom() {
		return map == null ? 1 : map.getMinimumZoomSupported();
	}
		
	@Override
	public void destroyLayer() {
		setMapTileAdapter(null);
		if (resourceManager != null) {
			resourceManager.removeMapTileLayerSize(this);
		}
	}

	public boolean isVisible() {
		return visible;
	}
	
	public void setVisible(boolean visible) {
		this.visible = visible;
	}
	
	public ITileSource getMap() {
		return map;
	}

}
