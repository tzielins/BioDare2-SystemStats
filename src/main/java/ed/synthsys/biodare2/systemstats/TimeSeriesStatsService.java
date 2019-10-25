/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ed.synthsys.biodare2.systemstats;

import com.fasterxml.jackson.databind.ObjectMapper;
import ed.biodare2.backend.repo.isa_dom.dataimport.DataTrace;
import ed.biodare2.backend.repo.isa_dom.exp.ExperimentalAssay;
import ed.biodare2.backend.repo.system_dom.SystemInfo;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 *
 * @author Tomasz Zielinski <tomasz.zielinski@ed.ac.uk>
 */
@Service
public class TimeSeriesStatsService {
    
    
    final Path storageDir;
    
    final  ObjectMapper mapper;
    
    @Autowired    
    public TimeSeriesStatsService(@Value("${bd2.storage.dir}") String storageDirPath, ObjectMapper mapper) {
        
        this.storageDir = Paths.get(storageDirPath);
        if (!Files.isDirectory(storageDir))
            throw new IllegalArgumentException("Storage path "+storageDir+" does not exists");
        
        this.mapper = mapper;
    };
    
    public List<ExpInfo> dataSetsStats() {
        
        try (Stream<ExpInfo> stats =  dataSetsStatsStream()) {
            return stats
                    .collect(Collectors.toList());
        }
    }
    
    public void saveDataSetsStats(Path file) throws IOException {
        
        String sep = "\t";
        try (Stream<ExpInfo> stats =  dataSetsStatsStream()) {
            
            List<ExpInfo> infos = stats.collect(Collectors.toList());
            
            try (BufferedWriter out = Files.newBufferedWriter(file)) {
                
                out.write(List.of(
                        "id","login","author","institution",
                        "size", "maxLength","maxDuration","points")
                        .stream().collect(Collectors.joining(sep)));
                out.newLine();
                
                infos.forEach( info -> {
                    try {
                        List<Object> row = new ArrayList<>();
                        row.addAll(List.of(info.id, info.creator, info.authorName, info.institution));
                        row.addAll(info.extractedDetails);
                        String line = row.stream()
                                .map(s -> s.toString())
                                .collect(Collectors.joining(sep));
                        out.write(line);
                        out.newLine();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }        
    }
    
    Stream<ExpInfo> dataSetsStatsStream() {
        
        List<Path> experiments = experimentsDirs().filter( exp -> hasData(exp))
                .collect(Collectors.toList());
        
        return experiments.parallelStream()
                .map( expDir -> readExpInfo(expDir))
                .map( expInfo -> {
                    
                    expInfo.extractedDetails.addAll(
                            datasetToStat(
                                    readData(dataSetFile(expInfo.expDir))
                            )
                    );
                    return expInfo;
                });
    }
    
    Stream<Path> experimentsDirs() {
        
        try {
            Path experiments = storageDir.resolve("experiments");
            if (!Files.isDirectory(experiments)) throw new IllegalArgumentException(experiments+" is not a directory");
            return Files.list(experiments);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    Stream<Path> experimentsDataDirs() {
        return experimentsDirs().parallel().map( dir -> dir.resolve("DATA2"));
    }
    
    Stream<Path> experimentsDataFiles() {
        return experimentsDataDirs()
                .map( dir -> dir.resolve("NO_DTR.ser"))
                .filter( f -> Files.isRegularFile(f));
    }
    
    Stream<List<DataTrace>> dataSets() {
        return experimentsDataFiles().map( f -> readData(f));
    }

    List<DataTrace> readData(Path file) {
        
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(file))) {
            
            return (List)in.readObject();
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e.getMessage(),e);
        } catch(IOException e) {
            throw new IllegalStateException("Cannot read data set: "+e.getMessage(),e);
        }        
    }
    
    List<Integer> datasetToStat(List<DataTrace> dataset) {
        
        int size = dataset.size();
        int maxLength = dataset.stream()
                .mapToInt(data -> data.trace.size())
                .max().orElse(0);
        int maxDurationHours = dataset.stream()
                .mapToInt(data -> (int)data.trace.getDuration())
                .max().orElse(0);
        
        int points = dataset.stream()
                .mapToInt(data -> data.trace.size())
                .sum();
        
        return List.of(size, maxLength, maxDurationHours, points);
        
    }
    
    Path dataSetFile(Path expDir) {
        return expDir.resolve("DATA2").resolve("NO_DTR.ser");
    }
    boolean hasData(Path expDir) {
        return Files.isRegularFile(dataSetFile(expDir));
    }
    
    ExpInfo readExpInfo(Path expDir) {
        
        try {
            long id = Long.parseLong(expDir.getFileName().toString());

            Path sysFile = expDir.resolve("SYSTEM").resolve(id+".sys.json");
            if (!Files.isRegularFile(sysFile)) throw new IllegalStateException("Missing sys file "+sysFile);
            Path expFile = expDir.resolve("ASSAY").resolve(id+".json");
            if (!Files.isRegularFile(expFile)) throw new IllegalStateException("Missing exp file "+expFile);

            SystemInfo sys = mapper.readValue(sysFile.toFile(), SystemInfo.class);
            
            ExperimentalAssay exp = mapper.readValue(expFile.toFile(), ExperimentalAssay.class);
            
            ExpInfo info = new ExpInfo();
            info.id = id;
            info.creator = sys.provenance.creation.actorLogin;
            info.authorName = exp.contributionDesc.authors.get(0).getName();
            info.institution = exp.contributionDesc.institutions.isEmpty() ? "unkown" : exp.contributionDesc.institutions.get(0).name;
            info.expDir = expDir;
            return info;
                    
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
    }
    
    static class ExpInfo {
        public long id;
        public String creator;
        public String authorName;
        public String institution;
        public List<Object> extractedDetails = new ArrayList<>();
        public Path expDir;
    }
    
    
}
