// vite.config.js
import { defineConfig } from "file:///F:/BaiduNetdiskDownload/idleschool/%E5%89%8D%E7%AB%AF%E4%BB%A3%E7%A0%81/SchoolSellVue3/node_modules/vite/dist/node/index.js";
import vue from "file:///F:/BaiduNetdiskDownload/idleschool/%E5%89%8D%E7%AB%AF%E4%BB%A3%E7%A0%81/SchoolSellVue3/node_modules/@vitejs/plugin-vue/dist/index.mjs";
import { fileURLToPath, URL } from "node:url";
var __vite_injected_original_import_meta_url = "file:///F:/BaiduNetdiskDownload/idleschool/%E5%89%8D%E7%AB%AF%E4%BB%A3%E7%A0%81/SchoolSellVue3/vite.config.js";
var vite_config_default = defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", __vite_injected_original_import_meta_url))
    }
  },
  server: {
    port: 5173,
    // 开发期代理：前端请求 /api/** → 后端 http://localhost:9321/**
    // 好处：① 免 CORS ② 前后端同源，生产只需改 VITE_API_BASE 指向真实域名
    proxy: {
      "/api": {
        target: "http://localhost:9321",
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, "")
      }
    }
  },
  build: {
    // 大依赖单独分包：echarts/element-plus 变更频率低，
    // 拆出 vendor chunk 后业务代码更新不影响浏览器缓存命中
    rollupOptions: {
      output: {
        manualChunks: {
          echarts: ["echarts"],
          "element-plus": ["element-plus", "@element-plus/icons-vue"]
        }
      }
    }
  }
});
export {
  vite_config_default as default
};
//# sourceMappingURL=data:application/json;base64,ewogICJ2ZXJzaW9uIjogMywKICAic291cmNlcyI6IFsidml0ZS5jb25maWcuanMiXSwKICAic291cmNlc0NvbnRlbnQiOiBbImNvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9kaXJuYW1lID0gXCJGOlxcXFxCYWlkdU5ldGRpc2tEb3dubG9hZFxcXFxpZGxlc2Nob29sXFxcXFx1NTI0RFx1N0FFRlx1NEVFM1x1NzgwMVxcXFxTY2hvb2xTZWxsVnVlM1wiO2NvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9maWxlbmFtZSA9IFwiRjpcXFxcQmFpZHVOZXRkaXNrRG93bmxvYWRcXFxcaWRsZXNjaG9vbFxcXFxcdTUyNERcdTdBRUZcdTRFRTNcdTc4MDFcXFxcU2Nob29sU2VsbFZ1ZTNcXFxcdml0ZS5jb25maWcuanNcIjtjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfaW1wb3J0X21ldGFfdXJsID0gXCJmaWxlOi8vL0Y6L0JhaWR1TmV0ZGlza0Rvd25sb2FkL2lkbGVzY2hvb2wvJUU1JTg5JThEJUU3JUFCJUFGJUU0JUJCJUEzJUU3JUEwJTgxL1NjaG9vbFNlbGxWdWUzL3ZpdGUuY29uZmlnLmpzXCI7aW1wb3J0IHsgZGVmaW5lQ29uZmlnIH0gZnJvbSAndml0ZSdcbmltcG9ydCB2dWUgZnJvbSAnQHZpdGVqcy9wbHVnaW4tdnVlJ1xuaW1wb3J0IHsgZmlsZVVSTFRvUGF0aCwgVVJMIH0gZnJvbSAnbm9kZTp1cmwnXG5cbi8vIGh0dHBzOi8vdml0ZWpzLmRldi9jb25maWcvXG5leHBvcnQgZGVmYXVsdCBkZWZpbmVDb25maWcoe1xuICBwbHVnaW5zOiBbdnVlKCldLFxuICByZXNvbHZlOiB7XG4gICAgYWxpYXM6IHtcbiAgICAgICdAJzogZmlsZVVSTFRvUGF0aChuZXcgVVJMKCcuL3NyYycsIGltcG9ydC5tZXRhLnVybCkpXG4gICAgfVxuICB9LFxuICBzZXJ2ZXI6IHtcbiAgICBwb3J0OiA1MTczLFxuICAgIC8vIFx1NUYwMFx1NTNEMVx1NjcxRlx1NEVFM1x1NzQwNlx1RkYxQVx1NTI0RFx1N0FFRlx1OEJGN1x1NkM0MiAvYXBpLyoqIFx1MjE5MiBcdTU0MEVcdTdBRUYgaHR0cDovL2xvY2FsaG9zdDo5MzIxLyoqXG4gICAgLy8gXHU1OTdEXHU1OTA0XHVGRjFBXHUyNDYwIFx1NTE0RCBDT1JTIFx1MjQ2MSBcdTUyNERcdTU0MEVcdTdBRUZcdTU0MENcdTZFOTBcdUZGMENcdTc1MUZcdTRFQTdcdTUzRUFcdTk3MDBcdTY1MzkgVklURV9BUElfQkFTRSBcdTYzMDdcdTU0MTFcdTc3MUZcdTVCOUVcdTU3REZcdTU0MERcbiAgICBwcm94eToge1xuICAgICAgJy9hcGknOiB7XG4gICAgICAgIHRhcmdldDogJ2h0dHA6Ly9sb2NhbGhvc3Q6OTMyMScsXG4gICAgICAgIGNoYW5nZU9yaWdpbjogdHJ1ZSxcbiAgICAgICAgcmV3cml0ZTogKHBhdGgpID0+IHBhdGgucmVwbGFjZSgvXlxcL2FwaS8sICcnKVxuICAgICAgfVxuICAgIH1cbiAgfSxcbiAgYnVpbGQ6IHtcbiAgICAvLyBcdTU5MjdcdTRGOURcdThENTZcdTUzNTVcdTcyRUNcdTUyMDZcdTUzMDVcdUZGMUFlY2hhcnRzL2VsZW1lbnQtcGx1cyBcdTUzRDhcdTY2RjRcdTk4OTFcdTczODdcdTRGNEVcdUZGMENcbiAgICAvLyBcdTYyQzZcdTUxRkEgdmVuZG9yIGNodW5rIFx1NTQwRVx1NEUxQVx1NTJBMVx1NEVFM1x1NzgwMVx1NjZGNFx1NjVCMFx1NEUwRFx1NUY3MVx1NTRDRFx1NkQ0Rlx1ODlDOFx1NTY2OFx1N0YxM1x1NUI1OFx1NTQ3RFx1NEUyRFxuICAgIHJvbGx1cE9wdGlvbnM6IHtcbiAgICAgIG91dHB1dDoge1xuICAgICAgICBtYW51YWxDaHVua3M6IHtcbiAgICAgICAgICBlY2hhcnRzOiBbJ2VjaGFydHMnXSxcbiAgICAgICAgICAnZWxlbWVudC1wbHVzJzogWydlbGVtZW50LXBsdXMnLCAnQGVsZW1lbnQtcGx1cy9pY29ucy12dWUnXVxuICAgICAgICB9XG4gICAgICB9XG4gICAgfVxuICB9XG59KVxuIl0sCiAgIm1hcHBpbmdzIjogIjtBQUE4WCxTQUFTLG9CQUFvQjtBQUMzWixPQUFPLFNBQVM7QUFDaEIsU0FBUyxlQUFlLFdBQVc7QUFGMEwsSUFBTSwyQ0FBMkM7QUFLOVEsSUFBTyxzQkFBUSxhQUFhO0FBQUEsRUFDMUIsU0FBUyxDQUFDLElBQUksQ0FBQztBQUFBLEVBQ2YsU0FBUztBQUFBLElBQ1AsT0FBTztBQUFBLE1BQ0wsS0FBSyxjQUFjLElBQUksSUFBSSxTQUFTLHdDQUFlLENBQUM7QUFBQSxJQUN0RDtBQUFBLEVBQ0Y7QUFBQSxFQUNBLFFBQVE7QUFBQSxJQUNOLE1BQU07QUFBQTtBQUFBO0FBQUEsSUFHTixPQUFPO0FBQUEsTUFDTCxRQUFRO0FBQUEsUUFDTixRQUFRO0FBQUEsUUFDUixjQUFjO0FBQUEsUUFDZCxTQUFTLENBQUMsU0FBUyxLQUFLLFFBQVEsVUFBVSxFQUFFO0FBQUEsTUFDOUM7QUFBQSxJQUNGO0FBQUEsRUFDRjtBQUFBLEVBQ0EsT0FBTztBQUFBO0FBQUE7QUFBQSxJQUdMLGVBQWU7QUFBQSxNQUNiLFFBQVE7QUFBQSxRQUNOLGNBQWM7QUFBQSxVQUNaLFNBQVMsQ0FBQyxTQUFTO0FBQUEsVUFDbkIsZ0JBQWdCLENBQUMsZ0JBQWdCLHlCQUF5QjtBQUFBLFFBQzVEO0FBQUEsTUFDRjtBQUFBLElBQ0Y7QUFBQSxFQUNGO0FBQ0YsQ0FBQzsiLAogICJuYW1lcyI6IFtdCn0K
