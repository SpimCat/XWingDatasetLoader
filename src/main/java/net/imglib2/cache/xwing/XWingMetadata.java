package net.imglib2.cache.xwing;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class XWingMetadata
{
	private final File directory;

	private final ArrayList< XWingMetadataItem > items;

	public static class XWingMetadataItem
	{
		private final int timepoint;
		private final double[] voxelDimensions;
		private final int[] stackDimensions;
		private final String rawFilename;

		public XWingMetadataItem(
				final int timepoint,
				final double[] voxelDimensions,
				final int[] stackDimensions,
				final String rawFilename
				)
		{
			this.timepoint = timepoint;
			this.voxelDimensions = voxelDimensions;
			this.stackDimensions = stackDimensions;
			this.rawFilename = rawFilename;
		}

		public int getTimepoint()
		{
			return timepoint;
		}

		public double[] getVoxelDimensions()
		{
			return voxelDimensions;
		}

		public int[] getStackDimensions()
		{
			return stackDimensions;
		}

		public String getRawFilename()
		{
			return rawFilename;
		}
	}

	public XWingMetadata( final File directory ) throws IOException
	{
		this.directory = directory;

		final List< IndexEntry > index = Files.lines( directory.toPath().resolve( "default.index.txt" ) )
				.map( XWingMetadata::parseIndexLine )
				.collect( Collectors.toList() );

		final List< MetaDataEntry > metadata = Files.lines( directory.toPath().resolve( "default.metadata.txt" ) )
				.map( XWingMetadata::parseMetadataLine )
				.collect( Collectors.toList() );

		for ( final MetaDataEntry m : metadata )
			for ( final IndexEntry i : index )
				if ( i.id == m.timepoint )
				{
					i.metadata = m;
					break;
				}

		for ( final IndexEntry i : index )
			if ( i.metadata == null  )
				throw new IllegalArgumentException("no metadata for " + i );

		items = new ArrayList<>();
		for ( final IndexEntry i : index )
			items.add( new XWingMetadataItem(
					i.id,
					i.metadata.voxelDims,
					i.dimensions,
					directory.toPath().resolve( String.format( "stacks/default/%06d.raw", i.id ) ).toFile().getAbsolutePath() ) );
	}

	public XWingMetadataItem get( final int index )
	{
		return items.get( index );
	}

	public int size()
	{
		return items.size();
	}

	public File getDirectory()
	{
		return directory;
	}

	// =============================================================

	private static IndexEntry parseIndexLine( final String line )
	{
		final String[] entries = line.split( "\t" );
		final int id = Integer.parseInt( entries[ 0 ] );
		final double timestamp = Double.parseDouble( entries[ 1 ] );
		final int[] dimensions = parseIntArray( entries[ 2 ] );
		return new IndexEntry( id, timestamp, dimensions );
	}

	private static int[] parseIntArray( final String text )
	{
		final String[] entries = text.split( ",\\s+" );
		if ( entries.length == 1 && entries[ 0 ].isEmpty() )
			return new int[ 0 ];
		final int[] array = new int[ entries.length ];
		for ( int i = 0; i < entries.length; ++i )
			array[ i ] = Integer.parseInt( entries[ i ] );
		return array;
	}

	private static MetaDataEntry parseMetadataLine( final String line )
	{
		final int start = line.indexOf( "{" ) + 1;
		final int end = line.indexOf( "}" ) - 1;
		final String[] entries = line.substring( start, end ).split( ",\\s+" );

		int timepoint = -1;
		final double[] voxelDims = new double[ 3 ];
		for ( final String entry : entries )
		{
			final String[] kv = entry.split( "\\s*=\\s*" );
			switch( kv[ 0 ] )
			{
			case "TimePoint":
				timepoint = Integer.parseInt( kv[ 1 ] );
				break;
			case "VoxelDimX":
				voxelDims[ 0 ] = Double.parseDouble( kv[ 1 ] );
				break;
			case "VoxelDimY":
				voxelDims[ 1 ] = Double.parseDouble( kv[ 1 ] );
				break;
			case "VoxelDimZ":
				voxelDims[ 2 ] = Double.parseDouble( kv[ 1 ] );
				break;
			}
		}

		if ( timepoint == -1 )
			throw new IllegalArgumentException();

		return new MetaDataEntry( timepoint, voxelDims );
	}

	private static class IndexEntry
	{
		public final int id;

		public final double timestamp;

		public final int[] dimensions;

		public MetaDataEntry metadata;

		public IndexEntry(
				final int id,
				final double timestamp,
				final int[] dimensions )
		{
			this.id = id;
			this.timestamp = timestamp;
			this.dimensions = dimensions;
		}

		@Override
		public String toString()
		{
			return "(id=" + id + ", timestamp=" + timestamp + ", dimensions" + Arrays.stream( dimensions ).mapToObj( Integer::toString ).collect( Collectors.joining( ", " ) ) + ")";
		}
	}

	private static class MetaDataEntry
	{
		public int timepoint;

		public double[] voxelDims;

		public MetaDataEntry(
				final int timepoint,
				final double[] voxelDims )
		{
			this.timepoint = timepoint;
			this.voxelDims = voxelDims;
		}

		@Override
		public String toString()
		{
			return "(timepoint=" + timepoint + ", voxelDims" + Arrays.stream( voxelDims ).mapToObj( Double::toString ).collect( Collectors.joining( ", " ) ) + ")";
		}
	}
}
