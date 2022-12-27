package dk.smlaursen.TSNCF;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import dk.smlaursen.TSNCF.output.Visualizer;
import dk.smlaursen.TSNCF.scheduler.IEEE802154;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.smlaursen.TSNCF.architecture.Node;
import dk.smlaursen.TSNCF.parser.TopologyParser;

public class Main {
	//Command line options
	private static final String NET_ARG = "net", DISP_ARG = "display";

	public static void main(String[] args){
		
		Option architectureFile = Option.builder(NET_ARG).required().argName("file").hasArg().desc("Use given file as network").build();
		
		Options options = new Options();
		options.addOption(architectureFile);
		options.addOption(DISP_ARG, false, "Display output");

		CommandLineParser parser = new DefaultParser();
		try {
			//Parse command line arguments
			CommandLine line = parser.parse(options, args);
			//Required, so cannot be null
			File net = new File(line.getOptionValue(NET_ARG));

			Logger logger = LoggerFactory.getLogger(Main.class.getSimpleName());
			boolean display = line.hasOption(DISP_ARG);
			
			//Parse Topology
			Graph<Node, DefaultEdge> graph= TopologyParser.parse(net);
			logger.info("Topology parsed!");

			/*
			for(DefaultEdge edge: graph.edgeSet()){
				System.out.println(edge);
			}
			*/

			int control_packet_number = 0;

			for(DefaultEdge edge: graph.edgeSet()){
				if (graph.getEdgeSource(edge).toString().equals("ES_R")){
					control_packet_number++;
				}
			}

			

			BufferedWriter writer = new BufferedWriter(new java.io.FileWriter("Result.txt"));
			writer.write("Graph Links. (First Node is Source Node : Second Node is Destination Node)\n");
			ArrayList<DefaultEdge> edgeList = new ArrayList<>(graph.edgeSet());
			writer.write(edgeList + "\n\n");

			ArrayList<ArrayList<DefaultEdge>> standardSchedule = new ArrayList<>();

			for(DefaultEdge edge: edgeList){
				ArrayList<DefaultEdge> slot = new ArrayList<>();
				slot.add(edge);
				standardSchedule.add(slot);
			}

			IEEE802154 standardDurations = new IEEE802154();

			double standardScheduleTime = standardDurations.getMacTsMaxAck() + standardDurations.getMacTsMaxTx() + standardDurations.getMacTsTxAckDelay() + standardDurations.getMacTsTxOffset();
			Map<ArrayList<DefaultEdge> , Double> standardScheduleDuration = new HashMap<>();
			for(ArrayList<DefaultEdge> forEdgeList: standardSchedule){
				standardScheduleDuration.put(forEdgeList, standardScheduleTime);
			}

			double total = 0;
			for(Map.Entry<ArrayList<DefaultEdge>, Double> entry: standardScheduleDuration.entrySet()){
				total += entry.getValue();
			}

			writer.write("127 B MAC.\n");
			writer.write("macTsMaxAck = " + standardDurations.getMacTsMaxAck() + " us.\n");
			writer.write("macTsMaxTx = " + standardDurations.getMacTsMaxTx() + " us.\n");
			writer.write("macTsTxAckDelay = " + standardDurations.getMacTsTxAckDelay() + " us.\n");
			writer.write("macTsTxOffset = " + standardDurations.getMacTsTxOffset() + " us.\n\n");

			writer.write("{} = slotframe. [] = slot.\n");
			writer.write(standardScheduleDuration + ", Total = " + total + " us.\n\n");

			ArrayList<Integer> controlPacketSizes = new ArrayList<>();
			controlPacketSizes.add(30);
			controlPacketSizes.add(50);
			controlPacketSizes.add(70);
			controlPacketSizes.add(90);

			Map<Integer, ArrayList<Double>> controlPacketDuration = new HashMap<>();
			for(Integer controlPacket: controlPacketSizes){
				ArrayList<Double> variableDurations = new ArrayList<>();
				double controlPacketTransmissionTime = controlPacket / (0.00025 * 125);
				variableDurations.add((standardDurations.getMacTsTxOffset() * controlPacketTransmissionTime) / 4256);
				variableDurations.add(controlPacketTransmissionTime);
				variableDurations.add((standardDurations.getMacTsTxAckDelay() * controlPacketTransmissionTime) / 4256);
				variableDurations.add((standardDurations.getMacTsMaxAck() * controlPacketTransmissionTime) / 4256);
				controlPacketDuration.put(controlPacket,variableDurations);
			}

			for(Map.Entry<Integer, ArrayList<Double>> entry: controlPacketDuration.entrySet()){
				writer.write("Control Packet Size = " + entry.getKey() + " B.\n");
				writer.write("macTsTxOffset = " + (standardDurations.getMacTsTxOffset() * entry.getValue().get(1)) / 4256 + "us.\n");
				writer.write("macTsMaxTx = " + entry.getValue().get(1) + "us.\n");
				writer.write("macTsTxAckDelay = " + standardDurations.getMacTsTxAckDelay() * entry.getValue().get(1) / 4256 + "us.\n");
				writer.write("macTsMaxAck = " + standardDurations.getMacTsMaxAck() * entry.getValue().get(1) / 4256 + "us.\n");
				double totalDuration = 0;
				for(Double duration: entry.getValue()){
					totalDuration += duration;
				}
				int slotInSlotNumber = (int) (10_000 / totalDuration);
				AtomicInteger counter = new AtomicInteger();

				ArrayList<ArrayList<DefaultEdge>> tempVariableSchedule = new ArrayList<>();
				for (DefaultEdge edge : edgeList) {
					if (counter.getAndIncrement() % slotInSlotNumber == 0) {
						tempVariableSchedule.add(new ArrayList<>());
					}
					tempVariableSchedule.get(tempVariableSchedule.size() - 1).add(edge);
				}

				ArrayList<ArrayList<DefaultEdge>> variableSchedule;
				if(tempVariableSchedule.get(tempVariableSchedule.size() - 1).size() < slotInSlotNumber){
					variableSchedule = new ArrayList<>(tempVariableSchedule.subList(0, tempVariableSchedule.size() - 1));
					int i = tempVariableSchedule.size() - 1;
					for (DefaultEdge edge : tempVariableSchedule.get(tempVariableSchedule.size() - 1)) {
						variableSchedule.add(new ArrayList<>());
						variableSchedule.get(i).add(edge);
						i++;
					}
				}
				else{
					variableSchedule = tempVariableSchedule;
				}

				int standardScheduleCounter = 0;
				writer.write("{\n");
				if(slotInSlotNumber == 1){
					for(ArrayList<DefaultEdge> slot: variableSchedule){
						writer.write(slot + "=" + standardScheduleTime + "(" + totalDuration * slot.size() + ", Wasting: " + (standardScheduleTime - totalDuration * slot.size()) + ")" + "\n");
						standardScheduleCounter++;
					}
					writer.write("}, Total = " + (standardScheduleCounter * standardScheduleTime) + " us.\n\n");
				}
				else{
					for(ArrayList<DefaultEdge> slot: variableSchedule){
						if (slot.size() == slotInSlotNumber){
							writer.write(slot + "=" + standardScheduleTime + "(" + totalDuration * slot.size() + ", Wasting: " + (standardScheduleTime - totalDuration * slot.size()) + ")" + "\n");

						}
						else {
							writer.write(slot + "=" + standardScheduleTime + "\n");
						}
						standardScheduleCounter++;
					}
					writer.write("}, Total = " + (standardScheduleTime * standardScheduleCounter) + " us.\n\n");
				}
			}

			writer.close();


			Visualizer vis = new Visualizer(graph);

			//Display Application?
			if(display){
				vis.topologyPanel();
			}

		} catch (ParseException | IOException e) {
			e.printStackTrace();

		}
	}
}
