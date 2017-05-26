package net.imglib2.cache.xwing;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import bdv.AbstractViewerSetupImgLoader;
import bdv.BigDataViewer;
import bdv.ViewerImgLoader;
import bdv.ViewerSetupImgLoader;
import bdv.cache.CacheControl;
import bdv.export.ProgressWriterConsole;
import bdv.img.cache.CacheArrayLoader;
import bdv.img.cache.VolatileCachedCellImg;
import bdv.img.cache.VolatileGlobalCellCache;
import bdv.spimdata.SequenceDescriptionMinimal;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import bdv.tools.InitializeViewerState;
import bdv.viewer.ViewerOptions;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.generic.sequence.ImgLoaderHint;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.TimePoints;
import mpicbg.spim.data.sequence.ViewId;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.volatiles.CacheHints;
import net.imglib2.cache.volatiles.LoadingStrategy;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileShortArray;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.volatiles.VolatileARGBType;
import net.imglib2.type.volatiles.VolatileUnsignedShortType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;

public class XWingSpimData
{
	public static SpimDataMinimal open( final File directory, final int[] cellDimensions, final int numFetcherThreads ) throws IOException
	{
		final XWingMetadata metadata = new XWingMetadata( directory );

		final HashMap< Integer, TimePoint > timepointMap = new HashMap<>();
		for ( int i = 0; i < metadata.size(); ++i )
			timepointMap.put( i, new TimePoint( i ) );

		final HashMap< Integer, BasicViewSetup > setupMap = new HashMap<>();
		final int setupId = 0;
		setupMap.put( setupId, new BasicViewSetup( setupId, null, null, null ) );

		final XWingImageLoader imgLoader = new XWingImageLoader( metadata, cellDimensions, numFetcherThreads );
		final SequenceDescriptionMinimal seq = new SequenceDescriptionMinimal( new TimePoints( timepointMap ), setupMap, imgLoader, null );


		final File basePath = directory;
		final HashMap< ViewId, ViewRegistration > registrations = new HashMap<>();
		for ( final TimePoint timepoint : seq.getTimePoints().getTimePointsOrdered() )
		{
			final int timepointId = timepoint.getId();
			final AffineTransform3D calib = new AffineTransform3D();
			final double[] voxelDimensions = metadata.get( timepointId ).getVoxelDimensions();
			calib.set( voxelDimensions[ 0 ], 0, 0 );
			calib.set( voxelDimensions[ 1 ], 1, 1 );
			calib.set( voxelDimensions[ 2 ], 2, 2 );
			registrations.put( new ViewId( timepointId, setupId ), new ViewRegistration( timepointId, setupId, calib ) );
		}
		final SpimDataMinimal spimData = new SpimDataMinimal( basePath, seq, new ViewRegistrations( registrations ) );

		return spimData;
	}

	public static class XWingVolatileShortArrayLoader implements CacheArrayLoader< VolatileShortArray >
	{
		private final XWingCellDataLoader dataLoader;

		public XWingVolatileShortArrayLoader( final XWingMetadata metadata )
		{
			dataLoader = new XWingCellDataLoader( metadata );
		}

		@Override
		public VolatileShortArray loadArray(
				final int timepoint,
				final int setup,
				final int level,
				final int[] dimensions,
				final long[] min ) throws InterruptedException
		{
			final short[] data = new short[ ( int ) Intervals.numElements( dimensions ) ];
			dataLoader.loadData( data, timepoint, dimensions, min );
			return new VolatileShortArray( data, true );
		}
	}

	public static class XWingImageLoader extends AbstractViewerSetupImgLoader< UnsignedShortType, VolatileUnsignedShortType > implements ViewerImgLoader
	{
		private final VolatileGlobalCellCache cache;

		private final double[][] mipmapResolutions;

		private final AffineTransform3D[] mipmapTransforms;

		private final XWingVolatileShortArrayLoader loader;

		private final XWingMetadata metadata;

		private final int[] cellDimensions;

		private final int numFetcherThreads;

		public XWingImageLoader(
				final XWingMetadata metadata,
				final int[] cellDimensions,
				final int numFetcherThreads )
		{
			super( new UnsignedShortType(), new VolatileUnsignedShortType() );
			cache = new VolatileGlobalCellCache( 1, numFetcherThreads );
			mipmapResolutions = new double[][] {{1,1,1}};
			mipmapTransforms = new AffineTransform3D[] { new AffineTransform3D() };
			loader = new XWingVolatileShortArrayLoader( metadata );
			this.metadata = metadata;
			this.cellDimensions = cellDimensions;
			this.numFetcherThreads = numFetcherThreads;
		}

		/**
		 * Create a {@link VolatileCachedCellImg} backed by the cache. The type
		 * should be either {@link ARGBType} and {@link VolatileARGBType}.
		 */
		protected < T extends NativeType< T > > VolatileCachedCellImg< T, VolatileShortArray > prepareCachedImage(
				final int timepointId,
				final int setupId,
				final int level,
				final LoadingStrategy loadingStrategy,
				final T type )
		{
			final long[] dimensions = Util.int2long( metadata.get( timepointId ).getStackDimensions() );
			final CellGrid grid = new CellGrid( dimensions, cellDimensions );

			final int priority = 0;
			final CacheHints cacheHints = new CacheHints( loadingStrategy, priority, false );
			return cache.createImg( grid, timepointId, setupId, level, cacheHints, loader, type );
		}

		@Override
		public RandomAccessibleInterval< VolatileUnsignedShortType > getVolatileImage( final int timepointId, final int level, final ImgLoaderHint... hints )
		{
			return prepareCachedImage( timepointId, 0, level, LoadingStrategy.BUDGETED, volatileType );
		}

		@Override
		public RandomAccessibleInterval< UnsignedShortType > getImage( final int timepointId, final int level, final ImgLoaderHint... hints )
		{
			return prepareCachedImage( timepointId, 0, level, LoadingStrategy.BLOCKING, type );
		}

		@Override
		public double[][] getMipmapResolutions()
		{
			return mipmapResolutions;
		}

		@Override
		public AffineTransform3D[] getMipmapTransforms()
		{
			return mipmapTransforms;
		}

		@Override
		public int numMipmapLevels()
		{
			return 1;
		}

		@Override
		public ViewerSetupImgLoader< ?, ? > getSetupImgLoader( final int setupId )
		{
			return this;
		}

		@Override
		public CacheControl getCacheControl()
		{
			return cache;
		}

		XWingMetadata getMetadata()
		{
			return metadata;
		}

		int[] getCellDimensions()
		{
			return cellDimensions;
		}

		int getNumFetcherThreads()
		{
			return numFetcherThreads;
		}
	}

	// test
	public static void main( final String[] args ) throws IOException, SpimDataException
	{
		final String name = "/Users/pietzsch/Desktop/nicola";
		final int[] cellDimensions = new int[] { 64, 64, 64 };
		final int numFetcherThreads = 1;
		final File directory = new File( name );
		if ( directory.isDirectory() )
		{
			final SpimDataMinimal spimData = open( directory, cellDimensions, numFetcherThreads );

			new XmlIoSpimDataMinimal().save( spimData, "/USers/pietzsch/Desktop/xwing.xml" );

			final BigDataViewer bdv = BigDataViewer.open( spimData, "BigDataViewer", new ProgressWriterConsole(), ViewerOptions.options() );
			InitializeViewerState.initBrightness( 0.001, 0.999, bdv.getViewer(), bdv.getSetupAssignments() );
		}
	}
}
