/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.client.rest;

import com.ignorelist.kassandra.dxvk.cache.pool.common.api.CacheStorage;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.DxvkStateCache;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.DxvkStateCacheEntry;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.DxvkStateCacheInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.ExecutableInfo;
import java.util.Set;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

/**
 *
 * @author poison
 */
public class DxvkCachePoolRestClient extends AbstractRestClient implements CacheStorage {

	private static final String PATH="pool";

	private static final GenericType<Set<ExecutableInfo>> TYPE_EXECUTABLE_INFO_SET=new GenericType<Set<ExecutableInfo>>() {
	};
	private static final GenericType<Set<DxvkStateCacheInfo>> TYPE_CACHE_INFO_SET=new GenericType<Set<DxvkStateCacheInfo>>() {
	};
	private static final GenericType<Set<DxvkStateCacheEntry>> TYPE_CACHE_ENTRY_SET=new GenericType<Set<DxvkStateCacheEntry>>() {
	};

	public DxvkCachePoolRestClient(String baseUrl) {
		super(baseUrl);
	}

	@Override
	protected WebTarget getWebTarget() {
		return super.getWebTarget().path(PATH);
	}

	public Set<DxvkStateCacheInfo> getCacheDescriptors(int version, Set<ExecutableInfo> executableInfos) {
		return getWebTarget()
				.path("cacheDescriptors")
				.path(Integer.toString(version))
				.request(MediaType.APPLICATION_JSON)
				.post(Entity.json(executableInfos), TYPE_CACHE_INFO_SET);
	}

	@Override
	public DxvkStateCacheInfo getCacheDescriptor(int version, ExecutableInfo executableInfo) {
		return getWebTarget()
				.path("cacheDescriptor")
				.path(Integer.toString(version))
				.request(MediaType.APPLICATION_JSON)
				.post(Entity.json(executableInfo), DxvkStateCacheInfo.class);
	}

	@Override
	public DxvkStateCache getCache(int version, ExecutableInfo executableInfo) {
		return getWebTarget()
				.path("stateCache")
				.path(Integer.toString(version))
				.request(MediaType.APPLICATION_JSON)
				.post(Entity.json(executableInfo), DxvkStateCache.class);
	}

	@Override
	public Set<DxvkStateCacheEntry> getMissingEntries(DxvkStateCacheInfo cacheInfo) {
		return getWebTarget()
				.path("missingCacheEntries")
				.request(MediaType.APPLICATION_JSON)
				.post(Entity.json(cacheInfo), TYPE_CACHE_ENTRY_SET);
	}

	@Override
	public void store(DxvkStateCache dxvkStateCache) {
		getWebTarget()
				.path("store")
				.request()
				.post(Entity.json(dxvkStateCache));
	}

	@Override
	public Set<ExecutableInfo> findExecutables(int version, String subString) {
		return getWebTarget()
				.path("cacheDescriptors")
				.path(Integer.toString(version))
				.path(subString)
				.request(MediaType.APPLICATION_JSON)
				.get(TYPE_EXECUTABLE_INFO_SET);
	}

	@Override
	public DxvkStateCacheInfo getCacheDescriptorForBaseName(int version, String baseName) {
		return getWebTarget()
				.path("cacheDescriptorForBaseName")
				.path(Integer.toString(version))
				.path(baseName)
				.request(MediaType.APPLICATION_JSON)
				.get(DxvkStateCacheInfo.class);
	}

}
