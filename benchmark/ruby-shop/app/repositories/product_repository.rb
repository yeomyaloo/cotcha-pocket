class ProductRepository
  SORT_COLUMNS = {
    'name' => 'name',
    'price' => 'price',
    'newest' => 'created_at DESC'
  }.freeze

  def all(sort)
    Product.select(:id, :name, :price).order(SORT_COLUMNS.fetch(sort.to_s, 'name'))
  end

  def search(keyword)
    Product.select(:id, :name, :price).where("name LIKE '%#{keyword}%'")
  end

  def find(id)
    Product.where('id = ?', id).first
  end
end
