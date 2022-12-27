package dk.smlaursen.TSNCF.parser;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.InputMismatchException;

import org.jgrapht.graph.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import dk.smlaursen.TSNCF.architecture.Node;

public class TopologyParser {
	
	public static AbstractBaseGraph<Node, DefaultEdge> parse(File f){
		AbstractBaseGraph<Node, DefaultEdge> graph = new SimpleDirectedGraph<>(DefaultEdge.class);

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		Document dom;

		try{
			DocumentBuilder db = dbf.newDocumentBuilder();
			dom = db.parse(f);
			Element docEle = dom.getDocumentElement();

			Element graphEle = (Element) docEle.getElementsByTagName("graph").item(0);
			Map<String, Node> nodeMap = new HashMap<>();

			String edgeDefault = graphEle.getAttribute("edgedefault");

			boolean isDirected = switch (edgeDefault) {
				case "directed" -> true;
				case "undirected" -> false;
				default -> throw new InputMismatchException("edgeDefault " + edgeDefault + " is not supported");
			};

			//Parse nodes and create graph-vertices accordingly
			NodeList nl = graphEle.getElementsByTagName("node");
			if(nl.getLength() > 0){
				for(int i = 0; i < nl.getLength(); i++){
					String nodeName = ((Element) nl.item(i)).getAttribute("id");
					nodeName = nodeName.toUpperCase();
					Node n = new Node(nodeName);
					nodeMap.put(nodeName, n);
					graph.addVertex(n);
				}
			}

			//Parse edges and create graph-edges accordingly
			nl = graphEle.getElementsByTagName("edge");
			if(nl.getLength() > 0){
				for(int i = 0; i < nl.getLength(); i++){
					String source = ((Element) nl.item(i)).getAttribute("source");
					source = source.toUpperCase();
					String target = ((Element) nl.item(i)).getAttribute("target");
					target = target.toUpperCase();
					graph.addEdge(nodeMap.get(source), nodeMap.get(target));
					if(!isDirected){
						graph.addEdge(nodeMap.get(target), nodeMap.get(source));
					}
				}
			}
			nodeMap.clear();
		} catch(ParserConfigurationException | SAXException | IOException pce){
			pce.printStackTrace();
		}
		return graph;
	}
}
