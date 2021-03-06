package main;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Properties;
import java.util.Stack;
import java.util.UUID;

import models.UrlDTO;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import util.PostgresConnector;
import util.DateUtilFunctions;

/*
 This class will help in building the job stack 
 that will be used by threads to work on
 */

public class JobController {
	private static String propertiesMain = "properties/property.properties";

	public static Stack<UrlDTO> buildJobs() {

		Stack<UrlDTO> jobStack = new Stack<UrlDTO>();
		Logger log = null;

		try {
			Properties PropertyHandler = new Properties();
			PropertyHandler.load(new FileInputStream(propertiesMain));
			String logPath = PropertyHandler.getProperty("logPath");
			PropertyConfigurator.configure(new FileInputStream(logPath));
			log = Logger.getLogger(JobController.class.getName());
			Connection postgresConn = PostgresConnector.getPostGresConnection();

			String dbTables = PropertyHandler.getProperty("dbTables");
			Properties dbTablesPropertyHandler = new Properties();
			dbTablesPropertyHandler.load(new FileInputStream(dbTables));
			log.info("db tables property file loaded");

			if (postgresConn == null) {
				log.error("Postgres connection object could not be instantiated");
			} else {
				log.info("Connection object to postgres created successfully");
			}
			/*
			 * This is the logic that builds the jobs based on the number of
			 * authorized apps
			 */
			int numberOfAccounts= Integer.parseInt(PropertyHandler
					.getProperty("numberOfAccounts"));

			int totalSearchQueries = numberOfAccounts 
					* Integer.parseInt(PropertyHandler
							.getProperty("numberOfSearchQueries"));

			int perTrendCalls = (int) (totalSearchQueries / Integer
					.parseInt(PropertyHandler.getProperty("totalTrends")));

			log.info("Building the stack of jobs");

			for (int jobPerTrends = 0; jobPerTrends < perTrendCalls; jobPerTrends++) {

				String selectTrends = "SELECT id,trend,date from "
						+ dbTablesPropertyHandler.getProperty("trends")
						+ " where date = '" + DateUtilFunctions.getCurrentDate() + "'";

				PreparedStatement stmt = postgresConn
						.prepareStatement(selectTrends);

				ResultSet rs = stmt.executeQuery();

				if (rs == null) {
					log.error("Was unable to fetch trends for the date"
							+ DateUtilFunctions.getCurrentDate()
							+ " , something has gone wrong ");
				} else {
					while (rs.next()) {
						/*
						 * type casting the object to UUID bad programming
						 */
						UUID id = (UUID) rs.getObject(1);
						String trend = rs.getString(2);
						String searchUrl = PropertyHandler
								.getProperty("searchUrl");
						trend = URLEncoder.encode(trend, "ISO-8859-1");
						searchUrl = searchUrl + trend + "&lang=en&count=100&result_type=recent";
						URL searchURL = new URL(searchUrl);
						UrlDTO urlObj = new UrlDTO(searchURL, id);
						jobStack.push(urlObj);

					}

				}

			}
			postgresConn.close();
		} catch (FileNotFoundException e) {
			log.error("Properties file path error");
		} catch (IOException e) {
			log.error(e.getMessage());
		} catch (ParseException e) {
			log.error("Error while parsing the date");
		} catch (SQLException e) {
			log.error(e.getMessage());
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}

		return jobStack;
	}
}
