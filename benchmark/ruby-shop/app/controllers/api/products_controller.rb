require 'digest'

module Api
  class ProductsController < ApplicationController
    skip_before_action :authenticate_user!

    def index
      products = repository.all(params[:sort])
      response.set_header('ETag', Digest::SHA1.hexdigest(products.to_json))
      render json: products
    end

    def search
      render json: repository.search(params[:q])
    end

    def show
      product = repository.find(params[:id])
      return head :not_found if product.nil?

      render json: product
    end

    private

    def repository
      @repository ||= ProductRepository.new
    end
  end
end
