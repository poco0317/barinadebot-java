package bar.barinade.barry.discord;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageHistory;

@Service
public class MessageHistoryService {
	
	private static final Logger m_logger = LoggerFactory.getLogger(MessageHistoryService.class);
	
	private static ConcurrentHashMap<Long, MessageHistory> historyCache = new ConcurrentHashMap<>();
	private static ConcurrentHashMap<Long, Boolean> historyPullsInProgress = new ConcurrentHashMap<>();
	
	public boolean isInProgress(Long id) {
		return historyPullsInProgress.getOrDefault(id, false);
	}
	
	public MessageHistory getChannelHistory(MessageChannel c) {
		long id = c.getIdLong();
		if (!historyCache.containsKey(id)) {
			historyCache.put(id, c.getHistory());
		}
		return historyCache.get(id);
	}
	
	public long getCacheSize(MessageChannel c) {
		return getChannelHistory(c).size();
	}
	
	/**
	 * Boring alias for the lambda driven cacheAllChannelHistory
	 */
	public void cacheAllChannelHistory(MessageChannel c) {
		cacheAllChannelHistory(c, null, null);
	}

	/**
	 * Queues a message history call which recurses this function until a stop condition is reached.
	 * Arg runAtEnd will run once at the end of the whole process, and takes a list of every message in the history as a parameter.
	 * Arg runEveryIter will run every time the function recurses - every 100 messages and takes a list of the last batch of retrieved messages as a parameter.
	 */
	public void cacheAllChannelHistory(MessageChannel c, Consumer<? super List<Message>> runAtEnd, Consumer<? super List<Message>> runEveryIter) {
		final Long chanId = c.getIdLong();
		if (!isInProgress(chanId)) {
			historyPullsInProgress.put(chanId, true);
			try {
				cacheAllChannelHistoryInternal(c, runAtEnd, runEveryIter);
			} catch (Exception e) {
				m_logger.error("Error running channel history cache: "+e.getMessage(), e);
				historyPullsInProgress.put(chanId, false);
				throw e;
			}
			historyPullsInProgress.put(chanId, false);
		} else {
			if (c.getJDA().getGuildChannelById(c.getId()) != null) {
				m_logger.info("Finished caching channel history : CHANNEL {} : GUILD {}", chanId, c.getJDA().getGuildChannelById(chanId).getGuild().getId());
			} else {
				m_logger.info("Finished caching channel history : CHANNEL {} : NO GUILD", chanId);
			}
		}
	}
	
	private void cacheAllChannelHistoryInternal(MessageChannel c, Consumer<? super List<Message>> runAtEnd, Consumer<? super List<Message>> runEveryIter) {
		final Long chanId = c.getIdLong();
		MessageHistory history = getChannelHistory(c);
		history.retrievePast(100).queue(messagelist -> {
			if (messagelist == null || messagelist.size() == 0) {
				try {
					// finished
					if (runAtEnd != null) {
						runAtEnd.accept(history.getRetrievedHistory());
					}
					if (c.getJDA().getGuildChannelById(c.getId()) != null) {
						m_logger.info("Finished caching channel history : CHANNEL {} : GUILD {}", chanId, c.getJDA().getGuildChannelById(chanId).getGuild().getId());
					} else {
						m_logger.info("Finished caching channel history : CHANNEL {} : NO GUILD", chanId);
					}
				} catch (Exception e) {
					m_logger.error("Error finishing channel history cache: "+e.getMessage(), e);
				}
			} else {
				try {
					if (runEveryIter != null) {
						runEveryIter.accept(messagelist);
					}
				} catch (Exception e) {
					m_logger.error("Error executing runEveryIter channel cache function : CHANNEL "+chanId, e);
				}
				// repeat
				cacheAllChannelHistoryInternal(c, runAtEnd, runEveryIter);
			}
		});
	}

}
