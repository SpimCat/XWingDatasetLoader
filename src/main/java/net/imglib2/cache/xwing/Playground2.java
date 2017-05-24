package net.imglib2.cache.xwing;

import static net.imglib2.cache.img.AccessFlags.VOLATILE;
import static net.imglib2.cache.img.PrimitiveType.SHORT;

import java.io.File;
import java.io.IOException;
import java.nio.ShortBuffer;

import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.volatiles.SharedQueue;
import net.imglib2.cache.Cache;
import net.imglib2.cache.img.ArrayDataAccessFactory;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.cache.img.CellLoader;
import net.imglib2.cache.img.LoadedCellCacheLoader;
import net.imglib2.cache.ref.GuardedStrongRefLoaderRemoverCache;
import net.imglib2.cache.ref.SoftRefLoaderCache;
import net.imglib2.img.Img;
import net.imglib2.img.NativeImg;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileShortArray;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.type.numeric.integer.UnsignedShortType;

public class Playground2
{
	public static void main( final String[] args ) throws IOException
	{
		final String name = "/Users/pietzsch/Desktop/nicola";
		final File directory = new File( name );
		if ( directory.isDirectory() )
		{
			final XWingMetadata metadata = new XWingMetadata( directory );

			final double[] calib = metadata.get( 0 ).getVoxelDimensions();

			final int[] dim0 = metadata.get( 0 ).getStackDimensions();
			final int numTimepoints = metadata.size();

			final long[] dimensions = new long[ 4 ];
			for ( int d = 0; d < 3; ++d )
				dimensions[ d ] = dim0[ d ];
			dimensions[ 3 ] = numTimepoints;

			final int[] cellDimensions = new int[ 4 ];
			for ( int d = 0; d < 3; ++d )
				cellDimensions[ d ] = 64; //dim0[ d ];
//			for ( int d = 0; d < 2; ++d )
//				cellDimensions[ d ] = dim0[ d ];
//			cellDimensions[ 2 ] = 1;
			cellDimensions[ 3 ] = 1;

			final CellGrid grid = new CellGrid( dimensions, cellDimensions );
			final UnsignedShortType type = new UnsignedShortType();

			final Cache< Integer, MemoryMappedStack > stackCache = new GuardedStrongRefLoaderRemoverCache< Integer, MemoryMappedStack >( 3 )
					.withLoader( t -> new MemoryMappedStack( metadata.get( t ) ) )
					.withRemover( ( t, stack ) -> stack.close() );

			final CellLoader< UnsignedShortType > loader = new CellLoader< UnsignedShortType >()
			{
				@Override
				public void load( final Img< UnsignedShortType > cell ) throws Exception
				{
					@SuppressWarnings( "unchecked" )
					final short[] data = ( short[] ) ( ( NativeImg< UnsignedShortType, ? extends ArrayDataAccess< ? > > ) cell ).update( null ).getCurrentStorageArray();

					final int t = ( int ) cell.min( 3 );
					final MemoryMappedStack stack = stackCache.get( t );

					final ShortBuffer buf = stack.getBuffer();

					final int minz = ( int ) cell.min( 2 );
					final int maxz = ( int ) cell.max( 2 );
					final int miny = ( int ) cell.min( 1 );
					final int maxy = ( int ) cell.max( 1 );

					final int celldimx = ( int ) cell.dimension( 0 );

					final int[] steps = stack.getSteps();
					final int ystep = steps[ 1 ];
					final int zstep = steps[ 2 ] - ( int ) cell.dimension( 1 ) * steps[ 1 ];

					int ibuf = ( int ) ( cell.min( 0 ) + steps[ 1 ] * cell.min( 1 ) + steps[ 2 ] * cell.min( 2 ) );
					int idata = 0;
					for ( int z = minz; z <= maxz; ++z, ibuf += zstep )
						for ( int y = miny; y <= maxy; ++y, ibuf += ystep, idata += celldimx )
						{
							buf.position( ibuf );
							buf.get( data, idata, celldimx );
						}
				}
			};
			final Cache< Long, Cell< VolatileShortArray > > cache = new SoftRefLoaderCache< Long, Cell< VolatileShortArray > >()
					.withLoader( LoadedCellCacheLoader.get( grid, loader, type, VOLATILE ) );
			final Img< UnsignedShortType > img = new CachedCellImg<>( grid, type, cache, ArrayDataAccessFactory.get( SHORT, VOLATILE ) );

//			new ImageJ();
//			ImageJFunctions.show( img, "img" );
//
			final SharedQueue queue = new SharedQueue( 1 );
//			BdvFunctions.show( VolatileViews.wrapAsVolatile( img, queue ), "img", BdvOptions.options().sourceTransform( 1, 1, 2.5 ) );
			BdvFunctions.show( img, "img", BdvOptions.options().sourceTransform( calib ) );
		}
	}
}
