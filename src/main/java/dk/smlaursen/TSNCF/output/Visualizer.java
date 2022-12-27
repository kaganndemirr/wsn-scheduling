package dk.smlaursen.TSNCF.output;

import java.awt.BorderLayout;
import java.awt.Color;

import java.util.Collection;


import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;


import org.jgrapht.Graph;
import org.jgrapht.ext.JGraphXAdapter;
import org.jgrapht.graph.DefaultEdge;


import com.mxgraph.layout.mxFastOrganicLayout;
import com.mxgraph.layout.mxParallelEdgeLayout;
import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGraphModel;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxStyleUtils;
import com.mxgraph.view.mxGraphView;

import dk.smlaursen.TSNCF.architecture.Node;

public class Visualizer{
    private final mxGraphComponent canvasComponent;
    private final JPanel zoomPanel = new JPanel(new BorderLayout());

    /**Setups the topology in a JFrame.
     * This call requires the libraries JGraphX and JGraphT-ext to be present on the classpath
     * @param g the {@link Graph} to display*/
    public Visualizer(final Graph<Node, DefaultEdge> g){
        JGraphXAdapter<Node, DefaultEdge> adapter = new JGraphXAdapter<>(g);
        canvasComponent = new mxGraphComponent(adapter);
        canvasComponent.getViewport().setOpaque(true);
        canvasComponent.getViewport().setBackground(Color.WHITE);
        mxGraphModel graphModel = (mxGraphModel) canvasComponent.getGraph().getModel();
        Collection<Object> cells = graphModel.getCells().values();
        //Filter to get endSystems
        Object[] nodes = mxGraphModel.filterCells(cells.toArray(), cell -> {
            if (cell instanceof mxCell mxc) {
                return mxc.getValue() instanceof Node;
            }
            return false;
        });

        //Filter to get edges
        Object[] edges = mxGraphModel.filterCells(cells.toArray(), cell -> {
            if (cell instanceof mxCell mxc) {
                return mxc.getValue() instanceof DefaultEdge;
            }
            return false;
        });

        mxStyleUtils.setCellStyles(graphModel, edges, mxConstants.STYLE_NOLABEL ,"1");
        mxStyleUtils.setCellStyles(graphModel, edges, mxConstants.STYLE_STROKECOLOR, "black");

        mxStyleUtils.setCellStyles(graphModel, nodes, mxConstants.STYLE_FILLCOLOR, "BAE4B2");

        //Disable editing of figure
        canvasComponent.setEnabled(false);
        new mxFastOrganicLayout(adapter).execute(adapter.getDefaultParent());
        new mxParallelEdgeLayout(canvasComponent.getGraph()).execute(adapter.getDefaultParent());
        JSlider slider = new JSlider(SwingConstants.VERTICAL, 5, 50, 10);
        zoomPanel.add(new JLabel("Zoom "), BorderLayout.NORTH);
        zoomPanel.add(slider, BorderLayout.CENTER);

        mxGraphView view = canvasComponent.getGraph().getView();
        slider.addChangeListener(e -> {
            JSlider source = (JSlider)e.getSource();
            if (!source.getValueIsAdjusting()) {
                double scale = source.getValue() /10.0;
                SwingUtilities.invokeLater(() -> view.setScale(scale));
            }
        });
    }

    public void topologyPanel(){
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame();
            frame.setLayout(new BorderLayout(50,50));
            frame.add(canvasComponent, BorderLayout.CENTER);
            frame.add(zoomPanel, BorderLayout.EAST);
            frame.setTitle("Topology Visualization");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.pack();
            frame.setVisible(true);
        });
    }
}