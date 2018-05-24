package com.dystify.kkdystrack.v2.manager;

import java.util.List;
import java.util.concurrent.ExecutorService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.event.EventListener;

import com.dystify.kkdystrack.v2.core.event.types.CostOverrideEvent;
import com.dystify.kkdystrack.v2.core.util.Util;
import com.dystify.kkdystrack.v2.dao.OverrideRuleDAO;
import com.dystify.kkdystrack.v2.model.OverrideRule;
import com.dystify.kkdystrack.v2.service.DBTask;

import javafx.application.Platform;
import javafx.collections.ObservableList;

public class OverrideRuleManager extends AbstractManager
{
	private Logger log = LogManager.getLogger(getClass());
	private ObservableList<OverrideRule> ruleTblContents;
	private OverrideRuleDAO ruleDao;
	private ExecutorService dbTaskQueue;

	public OverrideRuleManager() {
	}
	
	
	
	public void refreshRuleTableContents() {
		dbTaskQueue.submit(new DBTask("Refresh Cost Table", () -> {
			List<OverrideRule> rules = ruleDao.fetchAllRules();
			Platform.runLater(()-> {
				ruleTblContents.setAll(rules);
			});
		}));
	}
	
	
	@EventListener
	public void handleOverrideRuleTblUpdate(CostOverrideEvent event) {
		refreshRuleTableContents();
	}

	public void setRuleTblContents(ObservableList<OverrideRule> ruleTblContents) {
		this.ruleTblContents = ruleTblContents;
	}

	public void setRuleDao(OverrideRuleDAO ruleDao) {
		this.ruleDao = ruleDao;
	}



	public void setDbTaskQueue(ExecutorService dbTaskQueue) {
		this.dbTaskQueue = dbTaskQueue;
	}

}
