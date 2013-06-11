/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms.impl.handlers.configuration;

import it.geosolutions.geoserver.jms.events.ToggleProducer;
import it.geosolutions.geoserver.jms.impl.events.configuration.JMSGlobalModifyEvent;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ThreadPoolExecutor;

import javax.media.jai.TileCache;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.CoverageAccessInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.JAIInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.XStream;

/**
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 * 
 */
public class JMSGeoServerHandler extends
		JMSConfigurationHandler<JMSGlobalModifyEvent> {
	private static final long serialVersionUID = -6421638425464046597L;

	final static Logger LOGGER = LoggerFactory
			.getLogger(JMSGeoServerHandler.class);

	private final GeoServer geoServer;
	private final ToggleProducer producer;

	public JMSGeoServerHandler(GeoServer geo, XStream xstream, Class clazz,
			ToggleProducer producer) {
		super(xstream, clazz);
		this.geoServer = geo;
		this.producer = producer;
	}

	@Override
	protected void omitFields(final XStream xstream) {
		// omit not serializable fields
	}

	@Override
	public boolean synchronize(JMSGlobalModifyEvent ev) throws Exception {
		if (ev == null) {
			throw new IllegalArgumentException("Incoming object is null");
		}
		try {
			// LOCALIZE service
			final GeoServerInfo localObject = localizeGeoServerInfo(geoServer,
					ev);

			// disable the message producer to avoid recursion
			producer.disable();

			// save changes locally
			this.geoServer.save(localObject);

		} catch (Exception e) {
			if (LOGGER.isErrorEnabled())
				LOGGER.error(this.getClass()
						+ " is unable to synchronize the incoming event: " + ev);
			throw e;
		} finally {
			producer.enable();
		}
		return true;

	}

	/**
	 * Return the local GeoServerInfo updating its member with the ones coming
	 * from the passed GeoServerInfo
	 * 
	 * @param geoServer
	 *            the local GeoServer instance
	 * @param deserInfo
	 *            the de-serialized GeoServerInfo instance
	 * @return the updated and local GeoServerInfo
	 * 
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 *             {@link BeanUtilsBean.copyProperties}
	 * @throws IllegalArgumentException
	 *             if arguments are null
	 * 
	 */
	private static GeoServerInfo localizeGeoServerInfo(
			final GeoServer geoServer, final JMSGlobalModifyEvent ev)
			throws IllegalAccessException, InvocationTargetException,
			NoSuchMethodException {
		if (geoServer == null || ev == null)
			throw new IllegalArgumentException(
					"Wrong passed arguments are null");

		final GeoServerInfo localObject = geoServer.getGlobal();

		it.geosolutions.geoserver.jms.impl.utils.BeanUtils.smartUpdate(
				localObject, ev.getPropertyNames(), ev.getNewValues());

		// LOCALIZE with local objects
		final GeoServerInfo deserGeoServerInfo = ev.getSource();
		localObject.setContact(localizeContactInfo(geoServer,
				deserGeoServerInfo.getContact()));
		localObject.setCoverageAccess(localizeCoverageAccessInfo(geoServer,
				deserGeoServerInfo.getCoverageAccess()));
		// localize JAI
		localObject.setJAI(localizeJAIInfo(geoServer,
				deserGeoServerInfo.getJAI()));

		return localObject;
	}

	/**
	 * Return the local JAIInfo updating its member with the ones coming from
	 * the passed JAIInfo
	 * 
	 * @param geoServer
	 *            the local GeoServer instance
	 * @param deserInfo
	 *            the de-serialized JAIInfo instance
	 * @return the updated and local JAIInfo
	 * 
	 * @throws IllegalAccessException
	 *             {@link BeanUtilsBean.copyProperties}
	 * @throws InvocationTargetException
	 *             {@link BeanUtilsBean.copyProperties}
	 * @throws IllegalArgumentException
	 *             if arguments are null
	 */
	private static JAIInfo localizeJAIInfo(final GeoServer geoServer,
			final JAIInfo deserInfo) throws IllegalAccessException,
			InvocationTargetException {
		if (geoServer == null || deserInfo == null)
			throw new IllegalArgumentException(
					"Wrong passed arguments are null");
		// get local instance
		final JAIInfo info = geoServer.getGlobal().getJAI();
		// temporarily store tyleCache reference
		final TileCache sunTyleCache = info.getTileCache();
		// overwrite all members
		BeanUtils.copyProperties(info, deserInfo);
		// set tyle cache using stored reference
		info.setTileCache(sunTyleCache);

		return info;
	}

	/**
	 * Return the updated local ContactInfo object replacing all the members
	 * with the ones coming from the passed ContactInfo
	 * 
	 * @param geoServer
	 *            the local GeoServer instance
	 * @param deserInfo
	 *            the de-serialized JAIInfo instance
	 * @return the updated local ContactInfo.
	 * 
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws IllegalArgumentException
	 *             if arguments are null
	 */
	private static ContactInfo localizeContactInfo(final GeoServer geoServer,
			final ContactInfo deserInfo) throws IllegalAccessException,
			InvocationTargetException {
		if (geoServer == null || deserInfo == null)
			throw new IllegalArgumentException(
					"Wrong passed arguments are null");
		// get local instance
		final ContactInfo info = geoServer.getGlobal().getContact();

		// overwrite all members
		BeanUtils.copyProperties(info, deserInfo);

		return info;
	}

	/**
	 * Return the updated local CoverageAccessInfo object replacing all the
	 * members with the ones coming from the passed CoverageAccessInfo
	 * 
	 * @param geoServer
	 *            the local GeoServer instance
	 * @param deserInfo
	 *            the de-serialized JAIInfo instance
	 * @return the updated local CoverageAccessInfo.
	 * 
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws IllegalArgumentException
	 *             if arguments are null
	 */
	private static CoverageAccessInfo localizeCoverageAccessInfo(
			final GeoServer geoServer, final CoverageAccessInfo deserInfo)
			throws IllegalAccessException, InvocationTargetException {
		if (geoServer == null || deserInfo == null)
			throw new IllegalArgumentException(
					"Wrong passed arguments are null");

		// get local instance
		final CoverageAccessInfo info = geoServer.getGlobal()
				.getCoverageAccess();

		// store local reference
		final ThreadPoolExecutor executor = info.getThreadPoolExecutor();

		// overwrite all members
		BeanUtils.copyProperties(info, deserInfo);

		// set thread pool using stored reference
		info.setThreadPoolExecutor(executor);

		return info;
	}
}
