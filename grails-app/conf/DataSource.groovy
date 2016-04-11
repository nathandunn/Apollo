grails {
    neo4j {
        type = "embedded"
//        location = "/opt/apollo/neo4j"
    }
}

//dataSource {
//    pooled = true
//    jmxExport = true
//    driverClassName = "org.h2.Driver"
//    username = "sa"
//    password = ""
//}
////hibernate {
////    cache.use_second_level_cache=true
////    cache.use_query_cache=true
////    cache.provider_class='org.hibernate.cache.EhCacheProvider'
////    cache.region.factory_class = 'grails.plugin.cache.ehcache.hibernate.BeanEhcacheRegionFactory4' // For Hibernate 4.0 and higher
////
////}
//
//hibernate {
//    cache.use_second_level_cache = true
//    cache.use_query_cache = true
//    cache.provider_class='org.hibernate.cache.EhCacheProvider'
////    cache.region.factory_class = 'net.sf.ehcache.hibernate.BeanEhcacheRegionFactory' // Hibernate 3
//    cache.region.factory_class = 'org.hibernate.cache.ehcache.BeanEhcacheRegionFactory4' // Hibernate 4
//    singleSession = true // configure OSIV singleSession mode
//    flush.mode = 'manual' // OSIV session flush mode outside of transactional context
//}
//
//// environment specific settings
