package main;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import org.apache.http.HttpException;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.json.JSONException;

import twitter.GetTrends;

public class MrTimer extends TimerTask {

	private final static long oncePerDay = 1000 * 60 * 60 * 24;
	private final static int time = 06;

	private final static int minutes = 23;
	private static Logger log = null;
	private static String propertiesMain = "properties/property.properties";

	public void run() {
		try {
			OAuthConsumer consumerObj = JuliusCaesar.getConsumerObject();

			GetTrends.retrieveTrends(consumerObj);
			log.info("Trends for the day has been added to the database");
			JuliusCaesar.putConsumerObject(consumerObj);

		} catch (OAuthMessageSignerException | OAuthExpectationFailedException
				| OAuthCommunicationException | IOException | JSONException
				| SQLException | HttpException | ParseException e) {
			log.error("Something went wrong while retrieving trends", e);
		} catch (InterruptedException e) {
			log.error(
					"Something went wrong while putting/retrieveing the consumer object to the pool ",
					e);
		}
	}

	/*
	 * public void run(){ System.out.println(Calendar.getInstance().getTime());
	 * }
	 */

	private static Date getTommorowTime() {

		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, time);
		cal.set(Calendar.MINUTE, minutes);
		return cal.getTime();

	}

	public static void startTask() {
		try {
			Properties propertyHandler = new Properties();
			propertyHandler.load(new FileInputStream(propertiesMain));
			String logPath = propertyHandler.getProperty("logPath");
			PropertyConfigurator.configure(new FileInputStream(logPath));
			log = Logger.getLogger(MrTimer.class.getName());

			MrTimer task = new MrTimer();
			Timer timer = new Timer();
			timer.schedule(task, getTommorowTime(), oncePerDay);
		} catch (IOException e) {
			log.error("Properties file not found " + e);
		}
	}

}
