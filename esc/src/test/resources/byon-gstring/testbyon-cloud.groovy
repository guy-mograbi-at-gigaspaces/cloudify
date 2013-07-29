/***************
 * Cloud configuration file for the Bring-Your-Own-Node (BYON) cloud.
 * See org.cloudifysource.domain.cloud.Cloud for more details.
 *
 * @author noak
 *
 */

cloud {
	// Mandatory. The name of the cloud, as it will appear in the Cloudify UI.
	name = "byon"

	/********
	 * General configuration information about the cloud driver implementation.
	 */
	configuration {
		// The cloud-driver implementation class.
		className "org.cloudifysource.esc.driver.provisioning.byon.ByonProvisioningDriver"
		// Optional. The template name for the management machines. Defaults to the first template in the templates section below.
		managementMachineTemplate "SMALL_LINUX"
		// Optional. Indicates whether internal cluster communications should use the machine private IP. Defaults to true.
		connectToPrivateIp true
		//Indicates whether communications with the management servers should use the machine private IP.
		bootstrapManagementOnPublicIp false
	}

	/*************
	 * Provider specific information.
	 */
	provider {
		// Mandatory. The name of the provider.
		provider "byon"

		// Mandatory. The HTTP/S URL where cloudify can be downloaded from by newly started machines.
		cloudifyUrl "http://pc-lab25:8087/publish/gigaspaces.zip"
		// Mandatory. The prefix for new machines started for servies.
		machineNamePrefix "cloudify_agent_"
		// Optional. Defaults to true. Specifies whether cloudify should try to deploy services on the management machine.
		// Do not change this unless you know EXACTLY what you are doing.

		managementOnlyFiles ([])

		// Optional. Logging level for the intenal cloud provider logger. Defaults to INFO.
		sshLoggingLevel "INFO"
		// Mandatory. Name of the new machine/s started as cloudify management machines.
		managementGroup "cloudify_manager"
		// Mandatory. Number of management machines to start on bootstrap-cloud. In production, should be 2. Can be 1 for dev.
		numberOfManagementMachines 1

		reservedMemoryCapacityPerMachineInMB 1024
	}

	/*************
	 * Cloud authentication information
	 */
	user {

	}

	cloudCompute {
		/***********
		 * Cloud machine templates available with this cloud.
		 */
		templates ([
			SMALL_LINUX : computeTemplate{
				// Mandatory. Files from the local directory will be copied to this directory on the remote machine.
				remoteDirectory "/tmp/gs-files"
				// Optional. template-generic credentials. Can be overridden by specific credentials on each node, in the nodesList section.
				username "tgrid"
				password "tgrid"
				// Mandatory. All files from this LOCAL directory will be copied to the remote machine directory.
				localDirectory "upload"
				custom ([
					"nodesList" : ([
						([
							"id" : "byon-pc-lab{0}",
							"host-list" : "${IP}"
						])
					])
				])
			}
		])
	
	}

	/*****************
	 * Optional. Custom properties used to extend existing drivers or create new ones. 
	 */
	// Optional. Sets whether to delete the remoteDirectory created by the cloud driver, when shutting down.
	custom ([
		"cleanGsFilesOnShutdown": "false",
		"itemsToClean": ""
	])

}