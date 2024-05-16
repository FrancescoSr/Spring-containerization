package com.baeuldung.ls;

import com.baeuldung.ls.persistence.model.Project;
import com.baeuldung.ls.persistence.repository.IProjectRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.LocalDate;

@SpringBootApplication
public class LsApp implements ApplicationRunner {
    IProjectRepository projectRepository;

    public LsApp(IProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public static void main(final String... args) {
        SpringApplication.run(LsApp.class, args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        projectRepository.save(new Project("P1", LocalDate.now()));
        projectRepository.save(new Project("P2", LocalDate.now()));
        projectRepository.save(new Project("P3", LocalDate.now()));
    }
}