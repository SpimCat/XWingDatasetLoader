package net.imglib2.cache.xwing;

import static net.imglib2.cache.img.AccessFlags.VOLATILE;
import static net.imglib2.cache.img.PrimitiveType.SHORT;

import java.io.File;
import java.io.IOException;

import ij.ImagePlus;
import ij.measure.Calibration;
import net.imglib2.cache.Cache;
import net.imglib2.cache.img.ArrayDataAccessFactory;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.cache.img.CellLoader;
import net.imglib2.cache.img.LoadedCellCacheLoader;
import net.imglib2.cache.ref.SoftRefLoaderCache;
import net.imglib2.img.Img;
import net.imglib2.img.NativeImg;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileShortArray;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.integer.UnsignedShortType;

public class XWingVirtualStack
{
	public static Img< UnsignedShortType > open( final XWingMetadata metadata, final int[] cellDimensions )
	{
		final int[] dim0 = metadata.get( 0 ).getStackDimensions();
		final int numTimepoints = metadata.size();

		final long[] dimensions = new long[ 4 ];
		for ( int d = 0; d < 3; ++d )
			dimensions[ d ] = dim0[ d ];
		dimensions[ 3 ] = numTimepoints;

		final int[] cellDimensions4D = new int[ 4 ];
		for ( int d = 0; d < 3; ++d )
			cellDimensions4D[ d ] = cellDimensions[ d ];
		cellDimensions4D[ 3 ] = 1;

		final CellGrid grid = new CellGrid( dimensions, cellDimensions4D );
		final UnsignedShortType type = new UnsignedShortType();

		final XWingCellDataLoader dataLoader = new XWingCellDataLoader( metadata );
		final CellLoader< UnsignedShortType > loader = new CellLoader< UnsignedShortType >()
		{
			@Override
			public void load( final Img< UnsignedShortType > cell ) throws Exception
			{
				@SuppressWarnings( "unchecked" )
				final short[] data = ( short[] ) ( ( NativeImg< UnsignedShortType, ? extends ArrayDataAccess< ? > > ) cell ).update( null ).getCurrentStorageArray();

				final int[] dim = new int[] {
						( int ) cell.dimension( 0 ),
						( int ) cell.dimension( 1 ),
						( int ) cell.dimension( 2 )
				};
				final long[] min = new long[] {
						cell.min( 0 ),
						cell.min( 1 ),
						cell.min( 2 ),
				};
				final int timepoint = ( int ) cell.min( 3 );
				dataLoader.loadData( data, timepoint, dim, min );
			}
		};
		final Cache< Long, Cell< VolatileShortArray > > cache = new SoftRefLoaderCache< Long, Cell< VolatileShortArray > >()
				.withLoader( LoadedCellCacheLoader.get( grid, loader, type, VOLATILE ) );
		final Img< UnsignedShortType > img = new CachedCellImg<>( grid, type, cache, ArrayDataAccessFactory.get( SHORT, VOLATILE ) );

		return img;
	}

	public static ImagePlus open( final File directory, final int[] cellDimensions ) throws IOException
	{
		final XWingMetadata metadata = new XWingMetadata( directory );

		final Img< UnsignedShortType > img = open( metadata, cellDimensions );

		final ImagePlus imp = ImageJFunctions.show( img, directory.getName() );

		final double[] voxelDimensions = metadata.get( 0 ).getVoxelDimensions();
		final Calibration calibration = new Calibration();
		calibration.pixelWidth = voxelDimensions[ 0 ];
		calibration.pixelHeight = voxelDimensions[ 1 ];
		calibration.pixelDepth = voxelDimensions[ 2 ];
		imp.setCalibration( calibration );
		imp.setDimensions( 1, ( int) img.dimension( 2 ), ( int ) img.dimension( 3 ) );

		return imp;
	}

	// test
	public static void main( final String[] args ) throws IOException
	{
		final String name = "/Users/pietzsch/Desktop/nicola";
		final int[] cellDimensions = new int[] { 64, 64, 64 };
		final File directory = new File( name );
		if ( directory.isDirectory() )
		{
			open( directory, cellDimensions );
		}
	}
}
