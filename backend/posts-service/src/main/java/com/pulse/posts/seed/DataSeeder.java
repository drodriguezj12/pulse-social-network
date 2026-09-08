package com.pulse.posts.seed;

import com.pulse.posts.post.PostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Seeds a feed worth looking at: two dozen posts spread over the last few days,
 * with likes already in place, so the app is alive on first launch and the
 * cursor pagination actually has something to page through.
 *
 * <p>Author ids and aliases match auth-service's seeder by convention — neither
 * service calls the other at startup. The same data ships as db/seed.sql.
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final String MARIANA = "00000000-0000-0000-0000-000000000001";
    private static final String CARLOS = "00000000-0000-0000-0000-000000000002";
    private static final String VALENTINA = "00000000-0000-0000-0000-000000000003";
    private static final String ANDRES = "00000000-0000-0000-0000-000000000004";
    private static final String DANIELA = "00000000-0000-0000-0000-000000000005";

    /** The seeded post that carries the demo picture: the newest one, so it opens the feed. */
    private static final String ILLUSTRATED_POST = "10000000-0000-0000-0000-000000000001";

    /** A seeded post: fixed id, author, text, and how long ago it was published. */
    private record DemoPost(String id, String authorId, String alias, String message,
                            int minutesAgo, List<String> likedBy) {
    }

    private final PostRepository postRepository;
    private final JdbcTemplate jdbcTemplate;

    public DataSeeder(PostRepository postRepository, JdbcTemplate jdbcTemplate) {
        this.postRepository = postRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (postRepository.count() > 0) {
            log.info("Seed skipped: posts already present");
            return;
        }

        List<DemoPost> posts = demoPosts();
        for (DemoPost post : posts) {
            insertPost(post);
            for (String liker : post.likedBy()) {
                insertLike(post.id(), liker);
            }
        }
        attachDemoImage(ILLUSTRATED_POST);

        long likes = posts.stream().mapToLong(p -> p.likedBy().size()).sum();
        log.info("AUDIT seed_completed posts={} likes={}", posts.size(), likes);
    }

    /** Gives one seeded post a picture, so the feed shows the image support on first run. */
    private void attachDemoImage(String postId) {
        try {
            byte[] image = new ClassPathResource("seed/post-art.jpg").getContentAsByteArray();
            jdbcTemplate.update(
                    "INSERT INTO posts.post_images (post_id, content_type, image) VALUES (?, ?, ?)",
                    UUID.fromString(postId), "image/jpeg", image);
        } catch (IOException e) {
            log.warn("Demo image not seeded: {}", e.getMessage());
        }
    }

    private List<DemoPost> demoPosts() {
        return List.of(
                // The five posts below keep the ids the integration tests rely on.
                // 0001, 0002 and 0004 stay unliked on purpose: those tests assert
                // exact like counts right after the first like.
                new DemoPost("10000000-0000-0000-0000-000000000001", MARIANA, "marilo",
                        "Estrenando Pulse. Aviso: voy a publicar demasiadas fotos de atardeceres.",
                        6, List.of()),   // this one carries the seeded picture
                new DemoPost("10000000-0000-0000-0000-000000000002", CARLOS, "cgomez",
                        "Opinion impopular: los stored procedures estan infravalorados.",
                        19, List.of()),
                new DemoPost("10000000-0000-0000-0000-000000000003", VALENTINA, "valen",
                        "Plan de fin de semana: cafe, codigo y una buena playlist.",
                        44, List.of(MARIANA, CARLOS, DANIELA)),
                new DemoPost("10000000-0000-0000-0000-000000000004", ANDRES, "atorres",
                        "Esta noche subo el proyecto que llevo tres meses puliendo. Deseenme suerte.",
                        78, List.of()),
                new DemoPost("10000000-0000-0000-0000-000000000005", DANIELA, "danim",
                        "Ver los contadores moverse solos es sorprendentemente satisfactorio.",
                        132, List.of(MARIANA, VALENTINA)),

                new DemoPost("20000000-0000-0000-0000-000000000001", CARLOS, "cgomez",
                        "Tres horas depurando. Era un punto y coma. Siempre es un punto y coma.",
                        190, List.of(MARIANA, VALENTINA, ANDRES, DANIELA)),
                new DemoPost("20000000-0000-0000-0000-000000000002", VALENTINA, "valen",
                        "Terminando el rediseno del dashboard. Los degradados quedaron mejor de lo que esperaba.",
                        240, List.of(MARIANA, CARLOS)),
                new DemoPost("20000000-0000-0000-0000-000000000003", MARIANA, "marilo",
                        "Consejo del dia: si tu README no explica por que, no explica nada.",
                        305, List.of(CARLOS, VALENTINA, ANDRES)),
                new DemoPost("20000000-0000-0000-0000-000000000004", ANDRES, "atorres",
                        "Cambie las consultas del feed a paginacion por cursor y la diferencia se nota.",
                        380, List.of(CARLOS, DANIELA)),
                new DemoPost("20000000-0000-0000-0000-000000000005", DANIELA, "danim",
                        "Hoy aprendi que PostgreSQL distingue PROCEDURE de FUNCTION. Tiene todo el sentido.",
                        460, List.of(MARIANA, CARLOS, VALENTINA)),
                new DemoPost("20000000-0000-0000-0000-000000000006", CARLOS, "cgomez",
                        "El mejor refactor de la semana fue borrar codigo que ya nadie usaba.",
                        620, List.of(ANDRES)),
                new DemoPost("20000000-0000-0000-0000-000000000007", MARIANA, "marilo",
                        "Cafe numero tres y todavia es martes.",
                        780, List.of(VALENTINA, DANIELA)),
                new DemoPost("20000000-0000-0000-0000-000000000008", VALENTINA, "valen",
                        "Un test de integracion que levanta la base real vale por diez con mocks.",
                        910, List.of(MARIANA, CARLOS, ANDRES, DANIELA)),
                new DemoPost("20000000-0000-0000-0000-000000000009", ANDRES, "atorres",
                        "Migre todo a Docker Compose y ahora arranca con un solo comando. Paz mental.",
                        1080, List.of(CARLOS)),
                new DemoPost("20000000-0000-0000-0000-000000000010", DANIELA, "danim",
                        "Recordatorio: el WebSocket es una optimizacion, no la fuente de verdad.",
                        1260, List.of(MARIANA, ANDRES)),
                new DemoPost("20000000-0000-0000-0000-000000000011", MARIANA, "marilo",
                        "Nada como cerrar el viernes con la suite en verde.",
                        1500, List.of(CARLOS, VALENTINA, ANDRES, DANIELA)),
                new DemoPost("20000000-0000-0000-0000-000000000012", CARLOS, "cgomez",
                        "Leyendo sobre indices compuestos. El orden de las columnas importa mas de lo que parece.",
                        1800, List.of(VALENTINA)),
                new DemoPost("20000000-0000-0000-0000-000000000013", VALENTINA, "valen",
                        "Domingo de documentar decisiones tecnicas. Mi yo del futuro lo agradecera.",
                        2200, List.of(MARIANA, DANIELA)),
                new DemoPost("20000000-0000-0000-0000-000000000014", ANDRES, "atorres",
                        "El diseno tambien es una funcionalidad. Una app fea se siente rota.",
                        2600, List.of(MARIANA, CARLOS, VALENTINA)),
                new DemoPost("20000000-0000-0000-0000-000000000015", DANIELA, "danim",
                        "Empece a escribir los mensajes de commit como si alguien fuera a leerlos. Alguien lo hace.",
                        3100, List.of(ANDRES)),
                new DemoPost("20000000-0000-0000-0000-000000000016", MARIANA, "marilo",
                        "La parte dificil nunca es el codigo, es decidir que no construir.",
                        3800, List.of(CARLOS, VALENTINA, ANDRES, DANIELA)),
                new DemoPost("20000000-0000-0000-0000-000000000017", CARLOS, "cgomez",
                        "Primer dia usando atajos de teclado en serio. No hay vuelta atras.",
                        4600, List.of(MARIANA)),
                new DemoPost("20000000-0000-0000-0000-000000000018", VALENTINA, "valen",
                        "Pequena victoria: el bundle bajo de 400 a 97 kilobytes comprimidos.",
                        5600, List.of(CARLOS, ANDRES, DANIELA)),
                new DemoPost("20000000-0000-0000-0000-000000000019", ANDRES, "atorres",
                        "Hola mundo. Estrenando cuenta por aqui.",
                        7200, List.of(MARIANA, CARLOS)));
    }

    private void insertPost(DemoPost post) {
        jdbcTemplate.update("""
                        INSERT INTO posts.posts (id, author_id, author_alias, message, published_at)
                        VALUES (?, ?, ?, ?, now() - (? * INTERVAL '1 minute'))
                        """,
                UUID.fromString(post.id()), UUID.fromString(post.authorId()),
                post.alias(), post.message(), post.minutesAgo());
    }

    private void insertLike(String postId, String userId) {
        jdbcTemplate.update("""
                        INSERT INTO posts.likes (post_id, user_id) VALUES (?, ?)
                        ON CONFLICT (post_id, user_id) DO NOTHING
                        """,
                UUID.fromString(postId), UUID.fromString(userId));
    }
}
