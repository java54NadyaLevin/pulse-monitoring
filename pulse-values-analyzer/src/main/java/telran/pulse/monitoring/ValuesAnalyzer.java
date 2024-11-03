package telran.pulse.monitoring;

import java.util.Map;
import java.util.logging.*;
import com.amazonaws.services.dynamodbv2.*;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;

public class ValuesAnalyzer {
	static Logger logger = Logger.getLogger("LoggerAppl");
	static AmazonDynamoDB client = AmazonDynamoDBClientBuilder.defaultClient();
	static DynamoDB dynamo = new DynamoDB(client);
	static Table table = dynamo.getTable("pulse_abnormal_values");
	private PulseData pulseData;
	public void pulseAnalyzer(DynamodbEvent event, Context context) {
		setLogger();
		event.getRecords().forEach(r -> recordHandler(r));
	}

	private void recordHandler(DynamodbStreamRecord r) {
		Map<String, AttributeValue> map = r.getDynamodb().getNewImage();
		if (map == null) {
			System.out.println("No new image found");
		} else if (r.getEventName().equals("INSERT")) {
			pulseData = new PulseData(Integer.parseInt(map.get("patientId").getN()), 
			Integer.parseInt(map.get("value").getN()), 
			Long.parseLong(map.get("timestamp").getN()));
					logger.finer(pulseData.toString());
			if ( pulseData.pulseValue() < 50 || pulseData.pulseValue() > 190) {
				abnormalPulseHandler();
			}
		} else {
			System.out.println(r.getEventName());
		}
	};

	private void abnormalPulseHandler() {

		if (pulseData.pulseValue() < 50) {
			logger.info("LOW pulse value: patientId=" + pulseData.patientId() + ", pulseValue=" + pulseData.pulseValue());

		} else if (pulseData.pulseValue() > 190) {
			logger.info("HIGH pulse value: patientId=" + pulseData.patientId() + ", pulseValue=" + pulseData.pulseValue());
		}
		table.putItem(new Item().withPrimaryKey("patientId", pulseData.patientId())
				.withInt("pulseValue", pulseData.pulseValue()).withLong("timestamp", pulseData.timestamp()));
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
