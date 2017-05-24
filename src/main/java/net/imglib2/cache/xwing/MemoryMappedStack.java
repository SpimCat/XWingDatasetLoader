package net.imglib2.cache.xwing;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.FileChannel.MapMode;

import net.imglib2.cache.xwing.XWingMetadata.XWingMetadataItem;
import net.imglib2.util.IntervalIndexer;
import net.imglib2.util.Intervals;

public class MemoryMappedStack
{
	private final RandomAccessFile mmFile;

	private final int[] stackdim;

	private final int[] steps;

	private final ShortBuffer buf;

	public MemoryMappedStack( final XWingMetadataItem item ) throws IOException
	{
		stackdim = item.getStackDimensions();
		mmFile = new RandomAccessFile( item.getRawFilename(), "r" );
		steps = new int[ 3 ];
		IntervalIndexer.createAllocationSteps( stackdim, steps );
		final long bytesize = Intervals.numElements( stackdim ) * 2;
		final MappedByteBuffer in = mmFile.getChannel().map( MapMode.READ_ONLY, 0, bytesize );
		buf = in.asShortBuffer();
	}

	public int[] getDimensions()
	{
		return stackdim;
	}

	public int[] getSteps()
	{
		return steps;
	}

	public ShortBuffer getBuffer()
	{
		return buf.duplicate();
	}

	public void close()
	{
		try
		{
			mmFile.close();
		}
		catch ( final IOException e )
		{
			throw new RuntimeException( e );
		}
	}
}