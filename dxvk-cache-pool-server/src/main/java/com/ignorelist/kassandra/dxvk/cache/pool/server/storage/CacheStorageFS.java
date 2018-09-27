/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.server.storage;

import com.google.common.base.Equivalence;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Sets;
import com.google.common.io.BaseEncoding;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.DxvkStateCache;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.DxvkStateCacheInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.DxvkStateCacheEntryInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.ExecutableInfo;
import com.ignorelist.kassandra.dxvk.cache.pool.common.model.ExecutableInfoEquivalenceRelativePath;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Simple storage using the filesystem.
 *
 * FS layout: storageRoot / version / executable parent path / executable name / entry sha256
 *
 * @author poison
 */
public class CacheStorageFS implements CacheStorage {

	private static final Logger LOG=Logger.getLogger(CacheStorageFS.class.getName());
	private static final Pattern SHA_256_HEX_PATTERN=Pattern.compile("[0-9A-F]{16}", Pattern.CASE_INSENSITIVE);
	private static final BaseEncoding BASE16=BaseEncoding.base16();

	private final Equivalence<ExecutableInfo> equivalence=new ExecutableInfoEquivalenceRelativePath();

	private final Path storageRoot;
	private ConcurrentMap<Equivalence.Wrapper<ExecutableInfo>, DxvkStateCacheInfo> storageCache;

	public CacheStorageFS(Path storageRoot) {
		this.storageRoot=storageRoot;
	}

	private synchronized ConcurrentMap<Equivalence.Wrapper<ExecutableInfo>, DxvkStateCacheInfo> getStorageCache() throws IOException {
		if (null==storageCache) {
			ConcurrentMap<Equivalence.Wrapper<ExecutableInfo>, DxvkStateCacheInfo> m=new ConcurrentHashMap<>();
			ImmutableSet<String> versions=Files.list(storageRoot)
					.filter(Files::isDirectory)
					.map(Path::getFileName)
					.map(Path::toString)
					.collect(ImmutableSet.toImmutableSet());
			for (String versionString : versions) {
				try {
					final int version=Integer.parseInt(versionString);
					final Path versionDirectory=storageRoot.resolve(versionString);
					final ImmutableSetMultimap<Path, Path> entriesInRelativePath=Files.walk(versionDirectory)
							.filter(Files::isRegularFile)
							.filter(p -> SHA_256_HEX_PATTERN.matcher(p.getFileName().toString()).matches())
							.collect(ImmutableSetMultimap.toImmutableSetMultimap(p -> versionDirectory.relativize(p.getParent()), p -> p));
					entriesInRelativePath.asMap().entrySet().parallelStream()
							.map(e -> buildCacheDescriptor(e.getKey(), e.getValue(), version))
							.forEach(d -> m.put(equivalence.wrap(d.getExecutableInfo()), d));
					storageCache=m;
				} catch (Exception e) {
					LOG.log(Level.WARNING, null, e);
				}
			}
		}
		return storageCache;
	}

	private static DxvkStateCacheInfo buildCacheDescriptor(final Path relativePath, final Collection<Path> cacheEntryPaths, int version) {
		DxvkStateCacheInfo cacheInfo=new DxvkStateCacheInfo();
		cacheInfo.setVersion(version);
		ExecutableInfo ei=new ExecutableInfo(relativePath);
		cacheInfo.setExecutableInfo(ei);
		ImmutableSet<DxvkStateCacheEntryInfo> entryDescriptors=cacheEntryPaths.stream()
				.map(Path::getFileName)
				.map(Path::toString)
				.map(BASE16::decode)
				.map(h -> new DxvkStateCacheEntryInfo(h))
				.collect(ImmutableSet.toImmutableSet());
		cacheInfo.setEntries(entryDescriptors);
		final Optional<FileTime> lastModified=cacheEntryPaths.stream()
				.map(p -> {
					try {
						return Files.getLastModifiedTime(p);
					} catch (IOException ex) {
						throw new IllegalStateException("failed to get mtime for:"+p);
					}
				})
				.max(FileTime::compareTo);
		if (lastModified.isPresent()) {
			cacheInfo.setLastModified(lastModified.get().toInstant());
		}
		return cacheInfo;
	}

	@Override
	public Set<DxvkStateCacheInfo> getCacheDescriptors() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	public DxvkStateCacheInfo getCacheDescriptor(ExecutableInfo executableInfo) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public DxvkStateCache getCache(ExecutableInfo executableInfo) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void store(final DxvkStateCache cache) throws IOException {
		final ExecutableInfo executableInfo=cache.getExecutableInfo();
		final Path targetDirectory=storageRoot.resolve(executableInfo.getRelativePath());
		final Equivalence.Wrapper<ExecutableInfo> executableInfoWrapper=equivalence.wrap(executableInfo);
		DxvkStateCacheInfo descriptor=getStorageCache().computeIfAbsent(executableInfoWrapper, w -> {
			DxvkStateCacheInfo d=new DxvkStateCacheInfo();
			d.setVersion(cache.getVersion());
			d.setEntrySize(cache.getEntrySize());
			d.setExecutableInfo(executableInfo);
			d.setEntries(Sets.newConcurrentHashSet());
			return d;
		});

		Files.createDirectories(targetDirectory);
		cache.getEntries().parallelStream();
	}

	private Path buildTargetDirectory(DxvkStateCache cache) {
		final ExecutableInfo executableInfo=cache.getExecutableInfo();
		final Path targetPath=storageRoot
				.resolve(Integer.toString(cache.getVersion()))
				.resolve(executableInfo.getRelativePath());
		return targetPath;
	}

}
