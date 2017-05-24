package net.imglib2.cache.xwing;

import java.nio.ShortBuffer;

import net.imglib2.cache.UncheckedCache;
import net.imglib2.cache.ref.GuardedStrongRefLoaderRemoverCache;

public class XWingCellDataLoader
{
	private final UncheckedCache< Integer, MemoryMappedStack > stackCache;

	public XWingCellDataLoader( final XWingMetadata metadata )
	{
		stackCache = new GuardedStrongRefLoaderRemoverCache< Integer, MemoryMappedStack >( 3 )
				.withLoader( t -> new MemoryMappedStack( metadata.get( t ) ) )
				.withRemover( ( t, stack ) -> stack.close() )
				.unchecked();
	}

	public void loadData(
			final short[] data,
			final int timepoint,
			final int[] dimensions,
			final long[] min ) throws InterruptedException
	{
		final MemoryMappedStack stack = stackCache.get( timepoint );
		final ShortBuffer buf = stack.getBuffer();

		final int minz = ( int ) min[ 2 ];
		final int maxz = ( int ) min[ 2 ] + dimensions[ 2 ] - 1;
		final int miny = ( int ) min[ 1 ];
		final int maxy = ( int ) min[ 1 ] + dimensions[ 1 ] - 1;

		final int celldimx = dimensions[ 0 ];

		final int[] steps = stack.getSteps();
		final int ystep = steps[ 1 ];
		final int zstep = steps[ 2 ] - dimensions[ 1 ] * steps[ 1 ];

		int ibuf = ( int ) ( min[ 0 ] + steps[ 1 ] * min[ 1 ] + steps[ 2 ] * min[ 2 ] );
		int idata = 0;
		for ( int z = minz; z <= maxz; ++z, ibuf += zstep )
			for ( int y = miny; y <= maxy; ++y, ibuf += ystep, idata += celldimx )
			{
				buf.position( ibuf );
				buf.get( data, idata, celldimx );
			}
	}
}