package com.scc;

import com.scc.dao.DatabaseInitializer;
import com.scc.dao.UserDao;
import com.scc.ui.Authentication;
import com.scc.ui.InputHelper;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        DatabaseInitializer.initialize();

        Scanner sc = new Scanner(System.in);
        InputHelper.init(sc);
        UserDao userDao = new UserDao();

        new Authentication(userDao, sc).start();

        sc.close();
    }
}