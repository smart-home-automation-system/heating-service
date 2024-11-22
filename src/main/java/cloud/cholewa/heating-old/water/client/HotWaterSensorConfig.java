//package cloud.cholewa.heating.water.client;
//
//import jakarta.validation.constraints.NotNull;
//import org.springframework.boot.context.properties.ConfigurationProperties;
//import org.springframework.web.util.UriBuilder;
//
//@ConfigurationProperties("shelly.sensor.uni-hot-water")
//public record HotWaterSensorConfig(
//    @NotNull String scheme,
//    @NotNull String host
//) {
//    public UriBuilder getUriBuilder(final UriBuilder uriBuilder) {
//        return uriBuilder.scheme(scheme).host(host);
//    }
//}
