definition(
    name: "PlottWatt Connector",
    namespace: "bigpunk6",
    author: "Michael Kurtz",
    description: "Upload Power Meter data to PlottWatt",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
    section("Power Meter") {
        input "power", "capability.powerMeter", title: "Power Meter", required: false, multiple: true
    }
    section ("PlotWatt API") {
        input "apiKey", "text", title: "PlotWatt API Key", required:true
        input "meterId", "text", title: "Meter ID", required:true
        input "uploadCount", "number", title: "Upload after this many events", required:true
    }
}

def installed() {
    initialize()
}

def updated() {
    unsubscribe()
    initialize()
}

def initialize() {
    state.body = []
    subscribe(power, "power", powerEvent)
}

def powerEvent(evt) {
    sendEvent(evt)
}

private sendEvent(evt) {
	def kwatts = evt.value.toDouble()/1000
    def timeStamp = now().toString() [0..9]
    state.body << "${meterId},${kwatts},${timeStamp}"
    log.info "${evt.displayName} ${evt.name} is ${evt.value}W"
    log.info state.body.size()
    if (state.body.size()  >= uploadCount) {
        def body = state.body.collect { it }.join(',')
        def headers = [:] 
		def userpassascii = "${apiKey}:"
    	def userpass = "Basic " + userpassascii.encodeAsBase64().toString()
		headers.put("Authorization", userpass)
		def uri = "http://plotwatt.com/api/v2/push_readings"
            def params = [
                uri: uri,
                headers: headers,
                body: body                
            ] 
            
            log.debug "Posting last ${uploadCount} events to ${uri}"
        state.body = []
        httpPost(params) { response ->
            log.info "httpPost responce:${response.status}"
	    }
    }
}