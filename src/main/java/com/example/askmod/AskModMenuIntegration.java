package com.example.askmod;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Entrypoint dang ky man hinh cau hinh AskMod voi ModMenu.
 * Duoc khai bao trong fabric.mod.json o "entrypoints" -> "modmenu".
 *
 * Luu y: man hinh nay chi doc/ghi duoc config/askmod.json cua tien trinh
 * dang chay no. Trong Singleplayer, client va integrated server dung chung
 * 1 tien trinh nen hoat dong dung y. Voi dedicated server (nhieu nguoi
 * choi that su qua mang), client cua nguoi choi khong co quyen truy cap
 * file config tren may server - van phai dung lenh "/ask admin ..." cho
 * truong hop do.
 */
public class AskModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return AskConfigScreen::new;
	}
}
