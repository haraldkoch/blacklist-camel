package net.cfrq.blacklist;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;

import static org.apache.camel.model.rest.RestParamType.*;

@SuppressWarnings("unused")
public class BlacklistRoutes extends RouteBuilder {

    @SuppressWarnings("RedundantThrows")
    public void configure() throws Exception {

        // a few test routes just to make sure that our infrastructure is working
        rest("/say")
                .get("/hello").id("get-hello").to("direct:hello")
                .get("/bye").id("get-bye").consumes("application/json").to("direct:bye")
                .post("/bye").id("post-bye").to("mock:update");

        from("direct:hello").id("direct-hello")
                .transform().constant("Hello World");
        from("direct:bye").id("direct-bye")
                .transform().constant("Bye World");

        rest("/")
            .get("/").id("get-all").description("get all blacklist entries")
                .param().name("ip").type(query).description("IP address to search").dataType("string").endParam()
                .route()
                .convertBodyTo(String.class) // this prevents an annoying "java.io.IOException: Stream closed" error
                .choice()
                    .when(header("ip").isNotNull())
                        .to("sql:SELECT * FROM blacklist WHERE ip LIKE :#ip")
                .otherwise()
                        .to("sql:SELECT * FROM blacklist")
                .endRest()

            .get("/{id}").id("get-entry").description("fetch an entry by ID")
                .param().name("id").type(path).description("The id of the user to get").dataType("integer").endParam()
                .route()
                .convertBodyTo(String.class) // this prevents an annoying "java.io.IOException: Stream closed" error
                .to("sql:SELECT * FROM blacklist WHERE id=:#id")

                .choice()
                    .when().simple("${body?.size} < 1")
                        .log("no entry found for request ID ${header.id}")
                        .setBody(simple("no request found with request ID ${header.id}"))
                        .setHeader(Exchange.CONTENT_TYPE, constant("text/plain"))
                        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant("404"))
                    .otherwise()
                        .transform().simple("${body[0]}")
                .end()

                // f'ing java.sql.Timestamp
                .endRest()

            .post("/").id("create-entry").description("create a new blacklist entry")
                .param().name("body").type(body).description("The entry to create").endParam()
                .route()

                .onException(org.springframework.dao.DuplicateKeyException.class)
                    .log("attempt to insert duplicate IP ${body[ip]}")
                    .setBody(simple("IP address ${body[ip]} is already in the blocklist"))
                    .setHeader(Exchange.CONTENT_TYPE, constant("text/plain"))
                    .setHeader(Exchange.HTTP_RESPONSE_CODE, constant("404"))
                    .handled(true)
                .end()

                .to("sql:INSERT INTO blacklist (ip, type, date, description) VALUES (:#ip, :#type, :#date, :#description)")
                .transform().simple("inserted ${body} rows.")
                .endRest()

            .put("/{id}").id("update-entry").description("update existing entry")
                .route()
                .transform().simple("update entry ${headers.id} with ${body}")
                .endRest()

            .delete("/{id}").id("delete-entry").description("delete existing entry")
                .route()
                .transform().simple("delete entry ${headers.id}")
                .endRest();
    }
}
