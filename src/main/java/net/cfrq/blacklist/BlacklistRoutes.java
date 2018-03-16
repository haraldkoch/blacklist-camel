package net.cfrq.blacklist;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

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
                        .setHeader("ip", simple("%${header.ip}%"))
                        .to("sql:SELECT * FROM blacklist WHERE ip LIKE :#ip")
                    .otherwise()
                        .to("sql:SELECT * FROM blacklist")
                .endRest()

            .get("/{id}").id("get-entry").description("fetch an entry by ID")
                .param().name("id").type(path).description("The id of the user to get").dataType("integer").endParam()
                .route()
                .convertBodyTo(String.class) // this prevents an annoying "java.io.IOException: Stream closed" error
                .to("sql:SELECT * FROM blacklist WHERE id=:#id")

                // f'ing java.sql.Timestamp
                .process(new Processor(){
                    @SuppressWarnings("unchecked")
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        List<Map<String,Object>> results = (List<Map<String, Object>>) exchange.getIn().getBody();
                        for (Map<String, Object> result : results) {
                            Object date = result.get("date");
                            // YYYY-MM-DD HH:MM:SS[.fraction]
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                            result.put("date", sdf.format(date));
                        }

                        exchange.getIn().setBody(results);
                    }
                })
                .endRest()

            .post("/").id("create-entry").description("create a new blacklist entry")
                .param().name("body").type(body).description("The entry to create").endParam()
                .route()
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
