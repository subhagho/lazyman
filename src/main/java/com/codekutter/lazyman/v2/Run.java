package com.codekutter.lazyman.v2;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.codekutter.lazyman.common.Config;
import com.codekutter.lazyman.common.LogUtils;
import com.codekutter.lazyman.core.*;
import com.codekutter.lazyman.core.model.*;
import com.codekutter.lazyman.ui.Helper;
import com.codekutter.lazyman.ui.Viewer;
import com.codekutter.lazyman.v2.model.Journey;
import com.codekutter.lazyman.v2.model.Path;
import com.codekutter.lazyman.v2.model.Point;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.moeaframework.problem.tsplib.DataType;
import org.moeaframework.problem.tsplib.Tour;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Getter
@Setter
public class Run {
    @Parameter(names = {"--config", "-c"}, description = "Configuration Properties file.", required = true)
    private String config;
    @Parameter(names = {"--data", "-d"}, description = "TSP Input Data file.", required = true)
    private String tspData;
    @Parameter(names = {"--type", "-t"}, description = "TSP File type.", required = true)
    private String tspDataType;
    @Parameter(names = {"--tour", "-r"}, description = "Result tour file path.", required = false)
    private String tourfile;
    @Parameter(names = {"--view", "-v"}, description = "View output.")
    private boolean view = false;
    @Setter(AccessLevel.NONE)
    private DataReader reader;
    private double tourDistance = -1;

    private void run() {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(config));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(tspData));
        JourneyProcessor processor = null;
        try {
            Config.init(config);
            DataType type = DataType.TSP;
            if (!Strings.isNullOrEmpty(tspDataType)) {
                type = DataType.valueOf(tspDataType);
            }
            reader = new DataReader(tspData, type);
            reader.read();
            reader.load();

            if (!Strings.isNullOrEmpty(tourfile)) {
                reader.readTours(tourfile);
                if (reader.tours() != null && !reader.tours().isEmpty()) {
                    computeTourDistance(reader.tours(), reader.cache());
                }
            }
            RunIteration previous = null;
            int iterations = 0;
            int startIndex = 0;
            while (true) {
                RunIteration iteration = new RunIteration(iterations, startIndex, reader.cache());
                iteration.run();
                String outf = iteration.print();
                if (iteration.isCompleted()) {
                    LogUtils.info(getClass(),
                            String.format("Completed in [%d] iterations. [output=%s]", iterations, outf));
                    Journey journey = new Journey(iteration.points());
                    journey.load();
                    if (journey.isComplete()) {
                        previous = iteration;
                        break;
                    } else {
                        if (previous != null && iteration.compare(previous.points())) {
                            LogUtils.warn(getClass(), String.format("Stuck after [%d] iteration.", iterations));
                            break;
                        }
                        if (iterations > 1500) {
                            LogUtils.warn(getClass(), String.format("Breaking after [%d] iteration.", iterations));
                            previous = iteration;
                            break;
                        }
                        previous = iteration;
                        journey.check();
                        continue;
                    }
                } else if (previous != null) {
                    if (iteration.compare(previous.points())) {
                        LogUtils.warn(getClass(), String.format("Stuck after [%d] iteration.", iterations));
                        break;
                    }

                }
                if (iterations % 500 == 0) {
                    LogUtils.info(getClass(), String.format("Completed [%d] iterations...", iterations));
                }
                previous = iteration;
                previous.save();
                iterations++;
            }
            if (processor == null) {
                Journey journey = new Journey(previous.points());
                journey.load();
                printTour(previous, reader.filename(), journey);

                if (view) {
                    Helper.journey = journey;
                    Helper.tours = reader.tours();
                    Viewer.show();
                }
            } else {
                Journey journey = processor.journey();
                printTour(previous, reader.filename(), journey);

                if (view) {
                    Helper.journey = journey;
                    Helper.tours = reader.tours();
                    Viewer.show();
                }
            }
        } catch (Exception ex) {
            LogUtils.error(getClass(), ex);
            throw new RuntimeException(ex);
        }
    }

    private void computeTourDistance(List<Tour> tours, Cache cache) {
        Point lp = null;
        Tour t = tours.get(0);
        List<Point> points = cache.pointList(0);
        for (int index = 0; index < t.size(); index++) {
            int pi = t.get(index);
            Point p = points.get(pi - 1);
            if (lp != null) {
                Path path = lp.path(p.sequence());
                tourDistance += path.actualLength();
            }
            lp = p;
        }
    }

    private void printTour(RunIteration iteration,
                           String name,
                           Journey journey) throws Exception {
        String dir = Config.get().runInfo().createOutputDir("tour");
        File f = new File(name);
        String filename = String.format("%s.tour", f.getName());
        File file = new File(String.format("%s/%s", dir, filename));
        if (file.exists()) {
            file.delete();
        }

        try (FileOutputStream fos = new FileOutputStream(file)) {
            StringBuilder builder = new StringBuilder();
            builder.append("NAME : ").append(filename).append("\n");
            builder.append("TYPE : TOUR\n");
            builder.append("DIMENSION : ").append(iteration.points().size()).append("\n");
            builder.append("COMPUTED DISTANCE : ").append(journey.distance()).append("\n");
            builder.append("DISTANCE : ").append(tourDistance).append("\n");
            builder.append("COMPLETE : ").append(journey.isComplete()).append("\n");
            builder.append("TOUR_SECTION\n");
            if (journey.isComplete()) {
                LinkedList<Point> tour = journey.route().get(0);
                for (Point point : tour) {
                    builder.append(point.sequence() + 1).append("\n");
                }
            } else {
                int count = 0;
                for (LinkedList<Point> tour : journey.route()) {
                    builder.append("-----TOUR ").append(count).append("-----\n");
                    for (Point point : tour) {
                        builder.append(point.sequence() + 1).append("\n");
                    }
                    builder.append("------END ").append(count).append("-----\n");
                    count++;
                }
            }
            String s = builder.toString();
            fos.write(s.getBytes(StandardCharsets.UTF_8));
        }
        LogUtils.info(getClass(), String.format("Tour output : %s", file.getAbsolutePath()));
    }

    public static void main(String... argv) {
        try {
            Run r = new Run();
            JCommander.newBuilder().addObject(r).build().parse(argv);
            r.run();
        } catch (Throwable t) {
            LogUtils.error(Run.class, t);
            t.printStackTrace();
        }
    }
}
