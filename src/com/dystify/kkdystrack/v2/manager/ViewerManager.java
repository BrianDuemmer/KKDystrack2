package com.dystify.kkdystrack.v2.manager;

import com.dystify.kkdystrack.v2.dao.ViewerDAO;
import com.dystify.kkdystrack.v2.model.Viewer;

public class ViewerManager extends AbstractManager 
{
	private ViewerDAO viewerDao;
	
	private static String dystrackUserId;
	private static String dysUserId;

	public ViewerManager(ViewerDAO viewerDao) {
		this.viewerDao = viewerDao;
	}
	
	public final Viewer dystrackUser() {
		Viewer dystrack = viewerDao.getByUserId(dystrackUserId);
		if(dystrack == null)
			log.warn("Unable to get viewer information for K. K. Dystrack!");
		return dystrack;
	}
	
	
	public final Viewer dystifyzerUser() {
		Viewer dystrack = viewerDao.getByUserId(dysUserId);
		if(dystrack == null)
			log.warn("Unable to get viewer information for Dystifyzer!");
		return dystrack;
	}

	public static String getDystrackUserId() {
		return dystrackUserId;
	}

	public static String getDysUserId() {
		return dysUserId;
	}

	public static void setDystrackUserId(String dystrackUserId) {
		ViewerManager.dystrackUserId = dystrackUserId;
	}

	public static void setDysUserId(String dysUserId) {
		ViewerManager.dysUserId = dysUserId;
	}
}
