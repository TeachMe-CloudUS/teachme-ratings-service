package com.mongodb.starter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@EnableMongoRepositories
public class ApplicationStarter {
    public static void main(String[] args) {
        SpringApplication.run(ApplicationStarter.class, args);
    }
}

// @RestController
// class HelloController {

//     @GetMapping("/hola")
//     public String getName() {
//         return "TeachMe actúa como plataforma de enseñanza mediante cursos para estudiantes interesados. Los usuarios son personas que buscan realizar cursos, dichos cursos pueden ser cerficados profesionales que promuevan la inserción laboral.\r\n" + //
//                         "\r\n" + //
//                         "Microservicio de Valoraciones";
//     }
// }
