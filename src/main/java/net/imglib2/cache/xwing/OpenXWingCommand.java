package net.imglib2.cache.xwing;

import java.io.File;
import java.io.IOException;

import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

import bdv.BigDataViewer;
import bdv.export.ProgressWriterConsole;
import bdv.spimdata.SpimDataMinimal;
import bdv.tools.InitializeViewerState;
import bdv.viewer.ViewerOptions;
import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImageJ;

@Plugin( type = Command.class, menuPath = "XWing>Open XWing RAW Dataset" )
public class OpenXWingCommand implements Command
{
	public static String directory = "C:\\structure\\data\\xwing\\2017-11-15-14-08-58-36-robert_fusion_test2";

	public static String cellDimensions = "64, 64, 64";

	public static int numFetcherThreads = 1;

	public static String openTypeChoices[] = new String[] { "BigDataViewer Dataset", "Virtual Stack" };

	public static String dataset = "default";

	public static int openType = 0;

	@Override
	public void run()
	{
		final GenericDialogPlus gd = new GenericDialogPlus( "Open XWing dataset" );
		gd.addDirectoryField( "directory", directory, 25 );
		gd.addStringField( "cell sizes", cellDimensions, 25 );
		gd.addNumericField( "num threads", numFetcherThreads, 0 );
		gd.addChoice("Data set", new String[]{"C0L0","C0L1","C0L2","C0L3", "C1L0","C1L1","C1L2","C1L3", "default"}, dataset);
		gd.addChoice( "open_as", openTypeChoices, openTypeChoices[ openType ] );

		gd.showDialog();
		if ( gd.wasCanceled() )
			return;

		directory = gd.getNextString();
		cellDimensions = gd.getNextString();
		numFetcherThreads = ( int ) gd.getNextNumber();
		dataset = gd.getNextChoice();
		openType = gd.getNextChoiceIndex();

		// validate
		if ( ! new File( directory ).isDirectory() )
		{
			IJ.error( directory + " is not a directory" );
			return;
		}
		if ( ! new File( directory + "/" + dataset + ".index.txt" ).isFile() )
		{
			IJ.error( "default.index.txt not found" );
			return;
		}
		if ( ! new File( directory + "/" + dataset + ".metadata.txt" ).isFile() )
		{
			IJ.error( "default.metadata.txt not found" );
			return;
		}
		int[] cellDims;
		try
		{
			cellDims = parseIntArray( cellDimensions );
		}
		catch( final Exception e )
		{
			IJ.error( "cell sizes must be formatted as \"X, Y, Z\"" );
			return;
		}
		if ( cellDims.length != 3 )
		{
			IJ.error( "cell sizes must be formatted as \"X, Y, Z\"" );
			return;
		}
		if ( numFetcherThreads <= 0 )
		{
			IJ.error( "at least one loader thread required" );
			return;
		}

		if ( openType == 0 )
			openSpimData( directory, dataset, cellDims, numFetcherThreads );
		else
			openVirtualStack( directory, dataset, cellDims );
	}

	private void openSpimData( final String directory, String dataset, final int[] cellDims, final int numFetcherThreads )
	{
		try
		{
			final SpimDataMinimal spimData = XWingSpimData.open( new File( directory), dataset , cellDims, numFetcherThreads );
			final BigDataViewer bdv = BigDataViewer.open( spimData, "BigDataViewer", new ProgressWriterConsole(), ViewerOptions.options() );
			InitializeViewerState.initBrightness( 0.001, 0.999, bdv.getViewer(), bdv.getSetupAssignments() );
		}
		catch ( final IOException e )
		{
			e.printStackTrace();
			IJ.error( e.toString() );
		}
	}

	private void openVirtualStack( final String directory, String dataset, final int[] cellDims )
	{
		try
		{
			XWingVirtualStack.open( new File( directory), dataset , cellDims );
		}
		catch ( final IOException e )
		{
			e.printStackTrace();
			IJ.error( e.toString() );
		}
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

	// test
	public static void main( final String[] args )
	{
		ImageJ.main( args );
		new OpenXWingCommand().run();
	}
}
