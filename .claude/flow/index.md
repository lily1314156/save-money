index.html
→ fetch('/api/brands')
→ ApiController.getBrands()
→ CouponService.getAllBrands()
→ BrandsDao.selectAllBrands()
→ MyBatis → MySQL brands 表
→ 回傳 List<Brands> → 自動轉 JSON → 前端渲染