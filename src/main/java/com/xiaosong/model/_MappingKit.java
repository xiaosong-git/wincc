package com.xiaosong.model;

import com.jfinal.plugin.activerecord.ActiveRecordPlugin;

/**
 * Generated by JFinal, do not modify this file.
 * <pre>
 * Example:
 * public void configPlugin(Plugins me) {
 *     ActiveRecordPlugin arp = new ActiveRecordPlugin(...);
 *     _MappingKit.mapping(arp);
 *     me.add(arp);
 * }
 * </pre>
 */
public class _MappingKit {
	
	public static void mapping(ActiveRecordPlugin arp) {
		arp.addMapping("tb_accessrecord", "id", TbAccessrecord.class);
		arp.addMapping("tb_building_server", "serverId", TbBuildingServer.class);
		arp.addMapping("tb_companyuser", "id", TbCompanyuser.class);
		arp.addMapping("tb_device", "deviceId", TbDevice.class);
		arp.addMapping("tb_devicerelated", "id", TbDevicerelated.class);
		arp.addMapping("tb_failreceive", "failedId", TbFailreceive.class);
		arp.addMapping("tb_license", "licenseId", TbLicense.class);
		arp.addMapping("tb_network", "id", TbNetwork.class);
		arp.addMapping("tb_shareroom", "recordId", TbShareroom.class);
		arp.addMapping("tb_visitor", "id", TbVisitor.class);
		arp.addMapping("tb_ptinfo", "deviceIP", TbPtinfo.class);
		arp.addMapping("tb_statement", "id", TbStatement.class);
	}
}

