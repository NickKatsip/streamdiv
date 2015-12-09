import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.utils.Utils;

import java.io.File;

public class TopologyDriver {

    public static void main(String[] args) {
        TopologyBuilder builder = new TopologyBuilder();
        Config conf = new Config();
        String[] tweetSchema = {};
        String[] projectedSchema = { "date", "text" };
        String[] keywords = {};
        Integer k = 10;
        Double radius = Double.valueOf(10);
        Boolean batch = true;
        TweetFileProducer producer = new TweetFileProducer("data" + File.separator + "tweet_file_1.txt", tweetSchema,
                projectedSchema);
        builder.setSpout("source", new TweetSpout(producer), 1).setNumTasks(1);
        RelevancyFilter relevancyFilter = new RelevancyFilter(keywords);
        builder.setBolt("relevancy", new RelevancyBolt(relevancyFilter), 1).setNumTasks(1).shuffleGrouping("source");
        DiversityOperator diversityOperator = new DiversityOperator(k, radius, batch);
        builder.setBolt("diversity", new DiversityBolt(diversityOperator), 1).setNumTasks(1).shuffleGrouping("relevancy");
        conf.setDebug(true);
        conf.setNumWorkers(1);
        conf.setNumAckers(1);
        conf.put(Config.TOPOLOGY_WORKER_CHILDOPTS,
                "-Xmx4096m -XX:+UseConcMarkSweepGC -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:NewSize=128m " +
                        "-XX:CMSInitiatingOccupancyFraction=70 -XX:-CMSConcurrentMTEnabled -Djava.net.preferIPv4Stack=true"
        );
        LocalCluster localCluster = new LocalCluster();
        localCluster.submitTopology("streamdiv", conf, builder.createTopology());
        Utils.sleep(30000);
        localCluster.killTopology("streamdiv");
        localCluster.shutdown();
    }
}
