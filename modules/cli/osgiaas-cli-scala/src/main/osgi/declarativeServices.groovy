final SCR_NAMESPACE = "http://www.osgi.org/xmlns/scr/v1.3.0"

commonProperties = { ->
    property( name: 'service.vendor', value: 'com.athaydes' )
}

component( xmlns: SCR_NAMESPACE, name: 'scalaCommand', immediate: true ) {
    commonProperties()
    implementation( 'class': 'com.athaydes.osgiaas.cli.scala.ScalaCommand' )
    property( name: 'service.description', value: 'OSGiaaS Scala Command' )
    service {
        provide( 'interface': 'org.apache.felix.shell.Command' )
    }
}
