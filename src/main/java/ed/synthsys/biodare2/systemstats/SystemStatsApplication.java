/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ed.synthsys.biodare2.systemstats;

import static java.lang.System.exit;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
/**
 *
 * @author Tomasz Zielinski <tomasz.zielinski@ed.ac.uk>
 */
@SpringBootApplication
public class SystemStatsApplication implements CommandLineRunner {
    
    @Autowired
    TimeSeriesStatsService dataSetStats;
    
    public static void main(String[] args) {
        SpringApplication.run(SystemStatsApplication.class, args);
    }     

    
    @Override
    public void run(String... args) throws Exception {
        
        System.out.println("Serivce is "+dataSetStats);
        Path file = Paths.get("datasets_stats."+LocalDate.now().toString().replace(":", "-")+".tsv");
        System.out.println("Stats will be at: "+file.toAbsolutePath());
        dataSetStats.saveDataSetsStats(file);
        System.out.println("Finished");
        // exit(0);
    }
}
