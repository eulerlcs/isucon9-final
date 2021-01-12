package jp.zhimingsoft.www.isucon.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import jp.zhimingsoft.www.isucon.dao.Marker;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.logging.slf4j.Slf4jImpl;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;

@Configuration
@Slf4j
@MapperScan(basePackages = {"jp.zhimingsoft.www.isucon.dao"})
public class AppConfiguration {

    @Bean
    public RestTemplate restTemplate(ObjectMapper objectMapper) {
        RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder();
        return restTemplateBuilder.messageConverters(new MappingJackson2HttpMessageConverter(objectMapper)).build();
    }

//    @Bean("sqlSessionFactoryBean")
//    public SqlSessionFactory getSqlSessionFactory(DataSource dataSource) throws Exception {
//        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
//        bean.setDataSource(dataSource);
//        SqlSessionFactory factory = bean.getObject();
//        org.apache.ibatis.session.Configuration configuration = factory.getConfiguration();
//        configuration.setLogImpl(Slf4jImpl.class);
//        configuration.setMapUnderscoreToCamelCase(true);
//        return factory;
//    }
//
//    @Bean
//    public MapperScannerConfigurer mapperScannerConfigurer() {
//        MapperScannerConfigurer mapperScannerConfigurer = new MapperScannerConfigurer();
//        mapperScannerConfigurer.setBasePackage(Marker.class.getPackageName());
//        mapperScannerConfigurer.setSqlSessionFactoryBeanName("sqlSessionFactoryBean");
//        return mapperScannerConfigurer;
//    }
}
