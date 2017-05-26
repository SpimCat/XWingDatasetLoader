package net.imglib2.cache.xwing;

import static mpicbg.spim.data.XmlKeys.IMGLOADER_FORMAT_ATTRIBUTE_NAME;

import java.io.File;
import java.io.IOException;

import org.jdom2.Element;

import mpicbg.spim.data.XmlHelpers;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.ImgLoaderIo;
import mpicbg.spim.data.generic.sequence.XmlIoBasicImgLoader;
import net.imglib2.cache.xwing.XWingSpimData.XWingImageLoader;

@ImgLoaderIo( format = "xwing", type = XWingImageLoader.class )
public class XmlIoXWingImageLoader implements XmlIoBasicImgLoader< XWingImageLoader >
{
	public static final String DIRECTORY_TAG = "imagedirectory";
	public static final String CELL_DIMENSIONS_TAG = "celldimensions";
	public static final String NUM_FETCHER_THREADS_TAG = "numfetcherthreads";

	@Override
	public Element toXml( final XWingImageLoader imgLoader, final File basePath )
	{
		final Element elem = new Element( "ImageLoader" );
		elem.setAttribute( IMGLOADER_FORMAT_ATTRIBUTE_NAME, this.getClass().getAnnotation( ImgLoaderIo.class ).format() );

		elem.addContent( XmlHelpers.pathElement( DIRECTORY_TAG, imgLoader.getMetadata().getDirectory(), basePath ) );
		elem.addContent( XmlHelpers.intArrayElement( CELL_DIMENSIONS_TAG, imgLoader.getCellDimensions() ) );
		elem.addContent( XmlHelpers.intElement( NUM_FETCHER_THREADS_TAG, imgLoader.getNumFetcherThreads() ) );
		return elem;
	}

	@Override
	public XWingImageLoader fromXml( final Element elem, final File basePath, final AbstractSequenceDescription< ?, ?, ? > sequenceDescription )
	{
		try
		{
			final String path = XmlHelpers.loadPath( elem, DIRECTORY_TAG, basePath ).toString();
			final int[] cellDimensions = XmlHelpers.getIntArray( elem, CELL_DIMENSIONS_TAG );
			final int numFetcherThreads = XmlHelpers.getInt( elem, NUM_FETCHER_THREADS_TAG );
			final XWingMetadata metadata = new XWingMetadata( new File( path ) );
			return new XWingImageLoader( metadata, cellDimensions, numFetcherThreads );
		}
		catch ( final IOException e )
		{
			throw new RuntimeException( e );
		}
	}
}
