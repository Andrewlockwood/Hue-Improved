/**
 *  Hue Bulb
 *
 *  Copyright 2016 Alan Penner
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
metadata {
	definition (name: "Hue Bulb", namespace: "penner42", author: "Alan Penner") {
		capability "Switch Level"
		capability "Actuator"
		capability "Color Control"
        capability "Color Temperature"
		capability "Switch"
		capability "Polling"
		capability "Refresh"
		capability "Sensor"
        
        command "reset"
        command "updateStatus"

 		attribute "colorTemp", "number"
		attribute "bri", "number"
		attribute "sat", "number"
		attribute "reachable", "string"
		attribute "hue", "number"
		attribute "on", "string"
	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles (scale: 2){
		multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-single", backgroundColor:"#79b821", nextState:"-> Off"
				attributeState "off", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-single", backgroundColor:"#ffffff", nextState:"-> On"
				attributeState "-> On", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-single", backgroundColor:"#79b821", nextState:"-> Off"
				attributeState "-> Off", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-single", backgroundColor:"#ffffff", nextState:"-> On"
			}
			tileAttribute ("device.level", key: "SLIDER_CONTROL") {
				attributeState "level", action:"switch level.setLevel"
			}
			tileAttribute ("device.color", key: "COLOR_CONTROL") {
				attributeState "color", action:"setColor"
			}
		}

		standardTile("reset", "device.reset", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:"Reset Color", action:"reset", icon:"st.lights.philips.hue-single"
		}
		standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
		}
		valueTile("valueCT", "device.colorTemp", inactiveLabel: false, decoration: "flat", width: 2, height: 1) {
			state "colorTemp", label: 'Color Temp:  ${currentValue}'
        }
        controlTile("colorTemp", "device.colorTemp", "slider", inactiveLabel: false,  width: 4, height: 1, range:"(2000..6500)") { 
        	state "setCT", action:"setColorTemperature"
		}
	}
	main(["switch"])
	details(["switch","valueCT","colorTemp","refresh","reset"])
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
}

/** 
 * capability.switchLevel 
 **/
def setLevel(level) {
	def lvl = parent.scaleLevel(level, true)
	log.debug "Setting level to ${lvl}."
    
    def commandData = parent.getCommandData(device.deviceNetworkId)
	parent.sendHubCommand(new physicalgraph.device.HubAction(
    	[
        	method: "PUT",
			path: "/api/${commandData.username}/lights/${commandData.deviceId}/state",
	        headers: [
	        	host: "${commandData.ip}"
			],
	        body: [bri: lvl]
		])
	)    
}

/**
 * capability.colorControl 
 **/
def setColor(value) {
	def hue = parent.scaleLevel(value.hue, true, 65535)
    def sat = parent.scaleLevel(value.saturation, true, 254)
	log.debug "Setting color to [${hue}, ${sat}]"

	def commandData = parent.getCommandData(device.deviceNetworkId)
	parent.sendHubCommand(new physicalgraph.device.HubAction(
    	[
        	method: "PUT",
			path: "/api/${commandData.username}/lights/${commandData.deviceId}/state",
	        headers: [
	        	host: "${commandData.ip}"
			],
	        body: [hue: hue, sat: sat, bri: 254]
		])
	)    
    
}

def setHue(hue) {

}

def setSaturation(sat) {

}

/**
 * capability.colorTemperature 
 **/
def setColorTemperature(temp) {
	log.debug("Setting color temperature to ${temp}")
    def ct = Math.round(1000000/temp)
	def commandData = parent.getCommandData(device.deviceNetworkId)
	parent.sendHubCommand(new physicalgraph.device.HubAction(
    	[
        	method: "PUT",
			path: "/api/${commandData.username}/lights/${commandData.deviceId}/state",
	        headers: [
	        	host: "${commandData.ip}"
			],
	        body: [ct: ct]
		])
	)        
}

/** 
 * capability.switch
 **/
def on() {
	log.debug("Turning on!")
    
    def commandData = parent.getCommandData(device.deviceNetworkId)
	parent.sendHubCommand(new physicalgraph.device.HubAction(
    	[
        	method: "PUT",
			path: "/api/${commandData.username}/lights/${commandData.deviceId}/state",
	        headers: [
	        	host: "${commandData.ip}"
			],
	        body: [on: true, bri: 254]
		])
	)
}

def off() {
	log.debug("Turning off!")
    
    def commandData = parent.getCommandData(device.deviceNetworkId)
	parent.sendHubCommand(new physicalgraph.device.HubAction(
    	[
        	method: "PUT",
			path: "/api/${commandData.username}/lights/${commandData.deviceId}/state",
	        headers: [
	        	host: "${commandData.ip}"
			],
	        body: [on: false]
		])
	)
}

/** 
 * capability.polling
 **/
def poll() {

}

/**
 * capability.refresh
 **/
def refresh() {

}

def reset() {
	log.debug "Resetting color."
    def value = [level:100, hex:"#90C638", saturation:56, hue:23]
    setColor(value)
}

def updateStatus(action, param, val) {
	if (action == "state") {
		switch(param) {
        	case "on":
            	def onoff
            	if (val) { onoff = "on" } else { onoff = "off" }
            	sendEvent(name: "switch", value: onoff)
                break
            case "bri":
            	sendEvent(name: "level", value: parent.scaleLevel(val))
                break
			case "hue":
            	sendEvent(name: "hue", value: parent.scaleLevel(val, false, 65535))
                break
            case "sat":
            	sendEvent(name: "saturation", value: parent.scaleLevel(val))
                break
			case "ct": 
            	sendEvent(name: "colorTemp", value: Math.round(1000000/val))
                break
			default: 
				log.debug("Unhandled parameter: ${param}. Value: ${val}")    
        }
    }
}