/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ed.synthsys.biodare2.systemstats;

import ed.biodare2.backend.repo.isa_dom.dataimport.DataTrace;
import ed.robust.dom.data.TimeSeries;
import ed.synthsys.biodare2.systemstats.TimeSeriesStatsService.ExpInfo;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 *
 * @author Tomasz Zielinski <tomasz.zielinski@ed.ac.uk>
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class TimeSeriesStatsServiceTest {
    
    
    @Autowired
    TimeSeriesStatsService service;
    
    public TimeSeriesStatsServiceTest() {
    }
    
    @Before
    public void setUp() {
    }

    @Test
    public void testInitialezed() throws Exception {
        
        assertNotNull(service);
        assertNotNull(service.storageDir);
        assertTrue(Files.isDirectory(service.storageDir));
        
    }
    
    @Test
    public void experimentsGivesStream() {
        
        Stream<Path> experiments = service.experimentsDirs();
        assertEquals(10, experiments.count());
    }
    
    @Test
    public void experimentsDataFilesGivesCorrectStream() {
        
        List<Path> files = service.experimentsDataFiles()
                .collect(Collectors.toList());
        assertEquals(9, files.size());
        assertTrue(files.get(0).getFileName().toString().equals("NO_DTR.ser"));
    }   
    
    @Test
    public void readsDataSets() {
        List<List<DataTrace>> sets = service.dataSets().collect(Collectors.toList());
        
        assertEquals(9, sets.size());
        sets.forEach( set -> assertFalse(set.isEmpty()));
    }
    
    @Test
    public void dataSetToStats() {
        
        List<DataTrace> data = new ArrayList<>();
        
        DataTrace t = new DataTrace();
        t.trace = new TimeSeries(new double[]{0, 1, 24}, new double[]{1, 2, 3});
        data.add(t);
        
        t = new DataTrace();
        t.trace = new TimeSeries(new double[]{10, 11, 12, 13, 24}, new double[]{1, 2, 3, 4, 5});
        data.add(t);

        List<Integer> stats = service.datasetToStat(data);
        assertEquals(List.of(2,5,24,8), stats);
    }
    
    @Test
    public void dataSetsStats() {
        
        List<ExpInfo> stats = service.dataSetsStats();
        
        assertEquals(9, stats.size());
        stats.forEach(s -> assertTrue(s.id > 0));
        stats.forEach(s -> assertTrue((Integer)s.extractedDetails.get(0) > 0));
    }
    
    @Test
    public void savesStats() throws Exception {
        Path file = Paths.get("E:/Temp/data_stats.tsv");
        
        service.saveDataSetsStats(file);
        assertTrue(Files.exists(file));
        
    }
    
    @Test
    public void canExtractExpDetails() {
        
        Path expDir = Paths.get("E:\\Temp\\biodare2\\stats-storage-test\\experiments\\10155");
        if (!Files.exists(expDir)) throw new IllegalStateException("Missing test folder");
        
        ExpInfo info = service.readExpInfo(expDir);
        assertNotNull(info);
        assertEquals(10155, info.id);
        assertEquals("test", info.creator);
        assertEquals("Test User", info.authorName);
        
    }
            
    
}
