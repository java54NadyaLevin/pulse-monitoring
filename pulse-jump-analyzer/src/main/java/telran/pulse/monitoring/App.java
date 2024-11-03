package telran.pulse.monitoring;

import java.util.Map;
import java.util.logging.*;
import com.amazonaws.services.dynamodbv2.*;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;

public class App {
	static Logger logger = Logger.getLogger("LoggerAppl");
	static AmazonDynamoDB client = AmazonDynamoDBClientBuilder.defaultClient();
	static DynamoDB dynamo = new DynamoDB(client);
	static Table table = dynamo.getTable("pulse_abnormal_values");

	public void pulseAnalyzer(DynamodbEvent event, Context context) {
		setLogger();
		event.getRecords().forEach(r -> {
			Map<String, AttributeValue> map = r.getDynamodb().getNewImage();
			if (map == null) {
				System.out.println("No new image found");
			} else if (r.getEventName().equals("INSERT")) {
				int pulseValue = Integer.parseInt(map.get("value").getN());
				int patientId = Integer.parseInt(map.get("patientId").getN());
				long timestamp = Long.parseLong(map.get("timestamp").getN());
				logger.finer("patientId=" + patientId + " timestamp=" + timestamp
						+ " pulseValue=" + pulseValue);
				if ( pulseValue < 50 || pulseValue > 190) {
					abnormalPulseHandler(patientId, timestamp, pulseValue);
				}
			} else {
				System.out.println(r.getEventName());
			}
		});
	};

	private void abnormalPulseHandler(int patientId, long timestamp, int pulseValue) {

		if (pulseValue < 50) {
			logger.info("LOW pulse value: patientId=" + patientId + ", pulseValue=" + pulseValue);

		} else if (pulseValue > 190) {
			logger.info("HIGH pulse value: patientId=" + patientId + ", pulseValue=" + pulseValue);
		}
		table.putItem(new Item().withPrimaryKey("patientId", patientId)
				.withInt("pulseValue", pulseValue).withLong("timestamp", timestamp));
	}

	private void setLogger() {
		LogManager.getLogManager().reset();
		logger.setLevel(Level.FINER);
		Handler handlerConsole = new ConsoleHandler();
		handlerConsole.setFormatter(new SimpleFormatter());
		handlerConsole.setLevel(Level.FINEST);
		logger.addHandler(handlerConsole);
	}
}
