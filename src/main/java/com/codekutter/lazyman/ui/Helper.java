package com.codekutter.lazyman.ui;

import com.brunomnsilva.smartgraph.containers.SmartGraphDemoContainer;
import com.brunomnsilva.smartgraph.graph.Edge;
import com.brunomnsilva.smartgraph.graph.Graph;
import com.brunomnsilva.smartgraph.graph.GraphEdgeList;
import com.brunomnsilva.smartgraph.graph.Vertex;
import com.brunomnsilva.smartgraph.graphview.SmartGraphPanel;
import com.brunomnsilva.smartgraph.graphview.SmartGraphVertex;
import com.brunomnsilva.smartgraph.graphview.SmartPlacementStrategy;
import com.codekutter.lazyman.common.LogUtils;
import com.codekutter.lazyman.v2.model.Journey;
import com.codekutter.lazyman.v2.model.Path;
import com.codekutter.lazyman.v2.model.Point;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.moeaframework.problem.tsplib.Tour;

import java.util.*;

public class Helper {
    public static double minX = Double.MAX_VALUE;
    public static double minY = Double.MAX_VALUE;
    public static double maxX = Double.MIN_VALUE;
    public static double maxY = Double.MIN_VALUE;
    public static List<Tour> tours;
    public static Journey journey;

    @Setter
    @Getter
    @Accessors(fluent = true)
    public static class GraphView {
        Graph<Point, String> graph = new GraphEdgeList<>();
        SmartPlacementStrategy strategy = new PointPlacementStrategy();
        SmartGraphPanel<Point, String> view;
        List<Edge<String, Point>> tours;
    }

    public static GraphView build(@NonNull Journey journey) {
        GraphView view = new GraphView();
        Map<String, Vertex<Point>> points = new HashMap<>();
        Map<String, Edge<String, Point>> paths = new HashMap<>();
        for (LinkedList<Point> tour : journey.route()) {
            for (Point point : tour) {
                if (point.X() < Helper.minX) {
                    Helper.minX = point.X();
                }
                if (point.Y() < Helper.minY) {
                    Helper.minY = point.Y();
                }
                if (point.X() > Helper.maxX) {
                    Helper.maxX = point.X();
                }
                if (point.Y() > Helper.maxY) {
                    Helper.maxY = point.Y();
                }
                getVertex(point, view, points);
                for (Path path : point.connections()) {
                    if (path != null) {
                        addPath(path, view, paths, points);
                    }
                }
            }
        }
        if (tours != null && !tours.isEmpty() && tours.get(0) != null) {
            Point lp = null;
            Tour t = tours.get(0);
            view.tours = new ArrayList<>();
            for (int index = 0; index < t.size(); index++) {
                int pi = t.get(index);
                Point p = journey.points().get(pi - 1);
                if (lp != null) {
                    Vertex<Point> v1 = getVertex(lp, view, points);
                    Vertex<Point> v2 = getVertex(p, view, points);
                    Edge<String, Point> edge = view.graph.insertEdge(v1.element(), v2.element(),
                            String.format("%s>%s", lp.hashKey(), p.hashKey()));
                    view.tours.add(edge);
                }
                lp = p;
            }
        }
        return view;
    }

    private static Edge<String, Point> addPath(Path path, GraphView view,
                                               Map<String, Edge<String, Point>> paths, Map<String, Vertex<Point>> points) {
        String k = path.pathKey();
        Edge<String, Point> edge = paths.get(k);
        if (edge == null) {
            Vertex<Point> v1 = getVertex(path.A(), view, points);
            Vertex<Point> v2 = getVertex(path.B(), view, points);
            edge = view.graph.insertEdge(v1.element(), v2.element(), path.edgeString());
            paths.put(k, edge);
        }
        return edge;
    }

    private static Vertex<Point> getVertex(Point point, GraphView view, Map<String, Vertex<Point>> points) {
        Vertex<Point> pv = points.get(point.hashKey());
        if (pv == null) {
            pv = view.graph.insertVertex(point);
            points.put(point.hashKey(), pv);
        }
        return pv;
    }

    public static void show(@NonNull Journey journey) {
        GraphView view = build(journey);
        LogUtils.info(Helper.class, view.graph.toString());
        view.view = new SmartGraphPanel<>(view.graph, view.strategy);

        if (view.tours != null && !view.tours.isEmpty()) {
            for (Edge<String, Point> edge : view.tours) {
                view.view.getStylableEdge(edge).setStyleClass("edgeO");
            }
        }
        /*
        Basic usage:
        Use SmartGraphDemoContainer if you want zoom capabilities and automatic layout toggling
        */
        //Scene scene = new Scene(graphView, 1024, 768);
        Scene scene = new Scene(new SmartGraphDemoContainer(view.view), 1400, 800);

        Stage stage = new Stage(StageStyle.DECORATED);
        stage.setTitle("Salesman - Viewer");
        stage.setMinHeight(1080);
        stage.setMinWidth(768);
        stage.setScene(scene);
        stage.show();


        /*
        IMPORTANT: Must call init() after scene is displayed so we can have width and height values
        to initially place the vertices according to the placement strategy
        */
        view.view.init();
    }

    public static class PointPlacementStrategy implements SmartPlacementStrategy {

        /**
         * Implementations of placement strategies must implement this interface.
         * <p>
         * Should use the {@link SmartGraphVertex#setPosition(double, double) }
         * method to place individual vertices.
         *
         * @param width    Width of the area in which to place the vertices.
         * @param height   Height of the area in which to place the vertices.
         * @param theGraph Reference to the {@link Graph} containing the graph model.
         *                 Can use methods to check for additional information
         *                 pertaining the model.
         * @param vertices Collection of {@link SmartGraphVertex} to place.
         */
        @Override
        public <V, E> void place(double width, double height, Graph<V, E> theGraph, Collection<? extends SmartGraphVertex<V>> vertices) {
            for (SmartGraphVertex<V> vertex : vertices) {
                Vertex<Point> pv = (Vertex<Point>) vertex.getUnderlyingVertex();
                Point p = pv.element();
                double x = (p.X() - Helper.minX) * (width - 32) / (Helper.maxX - Helper.minX);
                double y = (p.Y() - Helper.minY) * (height - 32) / (Helper.maxY - Helper.minY);
                vertex.setPosition(x, y);
            }
        }
    }
}
