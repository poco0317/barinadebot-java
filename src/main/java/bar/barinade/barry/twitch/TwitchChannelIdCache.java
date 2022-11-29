package bar.barinade.barry.twitch;

import java.util.concurrent.ConcurrentHashMap;

public class TwitchChannelIdCache {

	private static final long STALE_ENTRY_MILLISEC = 1000L * 60L * 5L; // 5 minutes
	private static final int CACHE_MAX_SIZE = 50000; 
	
	ConcurrentHashMap<String, CacheEntry> nameToEntry = new ConcurrentHashMap<>();
	ConcurrentHashMap<Long, CacheEntry> idToEntry = new ConcurrentHashMap<>();
	
	public void flush() {
		nameToEntry.clear();
		idToEntry.clear();
	}
	
	/**
	 * Get a channel id using a channel name
	 */
	public String get(String channelName) {
		CacheEntry entry = nameToEntry.get(channelName);
		if (entry == null || entry.expired()) {
			return null;
		}
		return entry.getChannelId();
	}
	
	/**
	 * Get a channel name using a channel id
	 */
	public String get(Long channelId) {
		CacheEntry entry = idToEntry.get(channelId);
		if (entry == null || entry.expired()) {
			return null;
		}
		return entry.getChannelName();
	}
	
	public void put(String channelName, String channelId) {
		if (nameToEntry.size() > CACHE_MAX_SIZE || idToEntry.size() > CACHE_MAX_SIZE) {
			flush();
		}
		
		CacheEntry entry = new CacheEntry(channelName, channelId);
		nameToEntry.put(channelName, entry);
		idToEntry.put(Long.parseLong(channelId), entry);
	}
	
	public class CacheEntry {
		private long createdTimestamp;
		private String channelName;
		private String channelId;
		
		public CacheEntry() {
			createdTimestamp = System.currentTimeMillis();
		}
		public CacheEntry(String name, String id) {
			this();
			this.channelName = name;
			this.channelId = id;
		}
		
		public boolean expired() {
			return System.currentTimeMillis() - createdTimestamp >= STALE_ENTRY_MILLISEC;
		}
		public String getChannelName() {
			return channelName;
		}
		public String getChannelId() {
			return channelId;
		}
	}
}
